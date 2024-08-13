import git
import javalang
import datetime
from dateutil.relativedelta import relativedelta

# Set your Git repository path and Java file threshold here
REPO_PATH = "/Users/abhiram/Documents/TBE/evaluation_projects/hazelcast"
THRESHOLD = 25

def is_large_method(java_class, method):
    """
    Check if a given method has more than the specified number of lines.
    """
    method_body = parse(java_class.__code__).statements[method.name]
    return len(list(filter(lambda stmt: isinstance(stmt, parser.ast.ExprStmt) and
isinstance(stmt.value, parser.ast.Call), method_body))) > THRESHOLD

def find_new_large_methods(repo, commit):
    """
    Look for new methods added in the given Java file between two commits.
    """
    java_file = "path/to/your/java/file.java"
    old_content = repo.blob_tree_entry(commit.parent).data_stream.read().decode()
    new_content = repo.blob_tree_entry(commit).data_stream.read().decode()

    old_classes = parse(StringIO(old_content)).compilation_units[0].types
    new_classes = parse(StringIO(new_content)).compilation_units[0].types

    added_methods = []
    for old_class in old_classes:
        for new_class in new_classes:
            if old_class.name != new_class.name:
                continue

            old_methods = list(filter(lambda m: isinstance(m, parser.ast.FunctionDef) and
                    m.name not in added_methods, old_class.members))
            new_methods = list(filter(lambda m: isinstance(m, parser.ast.FunctionDef) and
                    m.name not in added_methods, new_class.members))

            for new_method in new_methods:
                if any((isinstance(m, parser.ast.FunctionDef) and m.name == new_method.name and
is_large_method(old_class, m)) for m in old_methods):
                    added_methods.append(new_method.name)
                    print(f'New large method: {new_method.name}')


def exists_large_function(code_str):
    try:
        tree = javalang.parse.parse(code_str)
    except javalang.parser.JavaSyntaxError:
        return False
    for path, node in tree.filter(javalang.tree.MethodDeclaration):
        if node.body is not None and len(node.body)+1 >= THRESHOLD:
            return True

def has_something_interesting(commit):
    # files_changes = commit.stats.files
    # java_src_files = [i for i in files_changes if (i.endswith(".java") and "test" not in i)]
    # for f in commit.stats.files:
    #     print(f)

    diffs = commit.diff(commit.parents)
    for d in diffs:
        if not d.a_path.endswith('.java') or "test" in d.a_path:
            continue
        if (
            d.change_type=='A' and
            exists_large_function(d.b_blob.data_stream.read().decode('utf-8'))):
            return True
        # print(d.a_blob.data_stream.read().decode('utf-8'))
        # print(d.b_blob.data_stream.read().decode('utf-8'))



def main():
    repo = git.Repo(REPO_PATH)
    branch = repo.active_branch
    g = repo.iter_commits(max_count=200)
    # You may want to filter commits based on some conditions here, e.g., using
    # `repo.commit_range()`.
    commit = branch.commit
    # find_new_large_methods(repo, commit)

    # parent_commit, child_commit = next(g), next(g)
    for commit in g:
        if commit.committed_datetime.date() < (datetime.date.today() - relativedelta(months=6)):
            break
        if has_something_interesting(commit):
            print(commit.hexsha)



if __name__ == "__main__":
    main()