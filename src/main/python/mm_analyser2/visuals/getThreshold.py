import os
from collections import defaultdict
import matplotlib.pyplot as plt
import numpy as np
import javalang
import sys
import logging

# Configure logging to capture parsing errors
logging.basicConfig(
    filename='parsing_errors.log',
    filemode='w',
    level=logging.ERROR,
    format='%(asctime)s - %(levelname)s - %(message)s'
)

def count_methods_in_class(class_node):
    """Recursively count methods in a class, including inner classes."""
    method_count = 0
    
    for child in class_node.body:
        # Count method declarations
        if isinstance(child, javalang.tree.MethodDeclaration):
            method_count += 1
        # Recursively count methods in inner classes
        elif isinstance(child, (javalang.tree.ClassDeclaration,
                                javalang.tree.InterfaceDeclaration,
                                javalang.tree.EnumDeclaration,
                                javalang.tree.AnnotationDeclaration)):
            method_count += count_methods_in_class(child)
    
    return method_count

def count_methods_in_file(file_path):
    """Count methods in all classes within a Java file."""
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
    except UnicodeDecodeError:
        # Try reading with a different encoding if utf-8 fails
        try:
            with open(file_path, 'r', encoding='latin-1') as file:
                content = file.read()
        except Exception as e:
            logging.error(f"Encoding error in file {file_path}: {e}")
            return {}
    except Exception as e:
        logging.error(f"Error reading file {file_path}: {e}")
        return {}
    
    try:
        # Parse the file content
        tree = javalang.parse.parse(content)
        class_method_counts = defaultdict(int)

        # Use tree.filter to iterate over all class declarations
        for path, node in tree.filter(javalang.tree.ClassDeclaration):
            class_name = node.name
            method_count = count_methods_in_class(node)
            class_method_counts[class_name] = method_count

        # Similarly, handle interfaces if needed
        for path, node in tree.filter(javalang.tree.InterfaceDeclaration):
            interface_name = node.name
            method_count = count_methods_in_class(node)
            class_method_counts[interface_name] = method_count

        # Similarly, handle enums if needed
        for path, node in tree.filter(javalang.tree.EnumDeclaration):
            enum_name = node.name
            method_count = count_methods_in_class(node)
            class_method_counts[enum_name] = method_count

        # Similarly, handle annotations if needed
        for path, node in tree.filter(javalang.tree.AnnotationDeclaration):
            annotation_name = node.name
            method_count = count_methods_in_class(node)
            class_method_counts[annotation_name] = method_count

        return class_method_counts
    except javalang.parser.JavaSyntaxError as e:
        logging.error(f"Syntax error parsing file {file_path}: {e}")
    except javalang.tokenizer.LexerError as e:
        logging.error(f"Lexer error parsing file {file_path}: {e}")
    except Exception as e:
        logging.error(f"Unexpected error parsing file {file_path}: {e}")
    
    return {}

def analyze_directory(directory):
    """Analyze a single directory and return a list of method counts per class."""
    all_class_sizes = []
    total_files = 0
    parsed_files = 0

    for root, _, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                total_files += 1
                file_path = os.path.join(root, file)
                class_sizes = count_methods_in_file(file_path)
                if class_sizes:
                    parsed_files += 1
                    all_class_sizes.extend(class_sizes.values())
    
    print(f"Processed {parsed_files}/{total_files} Java files in {directory}")
    return all_class_sizes

def plot_heavy_tail_distribution(project_quantiles, average_quantiles, quantiles):
    """Plot the heavy-tail distribution for each project and the average."""
    plt.figure(figsize=(14, 8))
    
    # Define a color palette
    colors = plt.cm.tab10.colors
    color_index = 0
    
    # Plot each project's distribution
    for project, quant_values in project_quantiles.items():
        plt.plot(quantiles, quant_values, label=project, color=colors[color_index % len(colors)], alpha=0.7)
        color_index += 1
    
    # Plot the average distribution
    plt.plot(quantiles, average_quantiles, 'k--', linewidth=2, label='Average')
    
    plt.xlabel('Quantiles', fontsize=14)
    plt.ylabel('Number of Methods', fontsize=14)
    plt.title('Heavy-tail Distribution for Class Sizes Across Projects', fontsize=16)
    plt.legend()
    plt.grid(True)
    plt.ylim(0, 100)  # Set y-axis range from 0 to 100
    plt.tight_layout()
    plt.show()

def main(directories):
    """Main function to analyze multiple directories and plot distributions."""
    quantiles = np.arange(0, 1.01, 0.01)
    project_quantiles = {}
    all_quantile_values = []
    
    for directory in directories:
        if not os.path.isdir(directory):
            print(f"Directory not found or inaccessible: {directory}")
            continue

        print(f"Analyzing project directory: {directory}")
        class_sizes = analyze_directory(directory)
        
        if not class_sizes:
            print(f"No classes found or all files failed to parse in directory: {directory}")
            continue
        
        # Compute quantile values for the current project
        quant_values = np.quantile(class_sizes, quantiles)
        project_name = os.path.basename(os.path.normpath(directory))
        project_quantiles[project_name] = quant_values
        all_quantile_values.append(quant_values)
        
        # Print some statistics for the current project
        print(f"Project: {project_name}")
        print(f"  Total classes analyzed: {len(class_sizes)}")
        print(f"  Average methods per class: {np.mean(class_sizes):.2f}")
        print(f"  Median methods per class: {np.median(class_sizes):.2f}")
        print(f"  10th percentile: {np.percentile(class_sizes, 10):.2f}")
        print(f"  20th percentile: {np.percentile(class_sizes, 20):.2f}")
        print(f"  30th percentile: {np.percentile(class_sizes, 30):.2f}")
        print(f"  40th percentile: {np.percentile(class_sizes, 40):.2f}")
        print(f"  50th percentile: {np.percentile(class_sizes, 50):.2f}")
        print(f"  60th percentile: {np.percentile(class_sizes, 60):.2f}")
        print(f"  70th percentile: {np.percentile(class_sizes, 70):.2f}")
        print(f"  80th percentile: {np.percentile(class_sizes, 80):.2f}")
        print(f"  90th percentile: {np.percentile(class_sizes, 90):.2f}")
        print(f"  95th percentile: {np.percentile(class_sizes, 95):.2f}\n")
    
    if not project_quantiles:
        print("No valid projects to analyze.")
        return
    
    # Compute the average quantile values across all projects
    average_quantiles = np.mean(all_quantile_values, axis=0)
    
    # Plot the distributions
    plot_heavy_tail_distribution(project_quantiles, average_quantiles, quantiles)
    print("Analysis complete. Check 'parsing_errors.log' for any parsing errors.")

if __name__ == "__main__":
    # Check if directories are provided as command-line arguments
    if len(sys.argv) > 1:
        # Directories are provided as arguments
        project_directories = sys.argv[1:]
    else:
        # Prompt the user to enter directories separated by commas
        input_dirs = input("Enter the paths to your Java project directories, separated by commas: ")
        project_directories = [d.strip() for d in input_dirs.split(",") if d.strip()]
    
    if not project_directories:
        print("No directories provided. Exiting.")
        sys.exit(1)
    
    main(project_directories)
