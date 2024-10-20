from github import Github
from datetime import datetime
import os
TOKEN = os.environ.get("GITHUB_TOKEN")

# Your GitHub personal access token
g = Github(TOKEN)

date_input = "Jan 2024"

# Search query
query = "stars:>1000 language:java"

# Get search results
repositories = g.search_repositories(query=query, sort="stars", order="desc")

# Filter repositories based on commit count
top_repos = []
for repo in repositories:
    try:
        commits_since_input_date = list(repo.get_commits(since=datetime(2024, 1, 1)))

        if commits_since_input_date:
            commit_count = len(commits_since_input_date)

            if commit_count >= 500:
                first_commit_id = commits_since_input_date[-1].sha  # Get first commit on/after Oct 2023
                latest_commit_id = commits_since_input_date[0].sha  # Get latest commit ID
                top_repos.append((repo, latest_commit_id, first_commit_id, commit_count))

            if len(top_repos) == 25:
                break
    except Exception as e:
        print(f"Error processing repository {repo.full_name}: {str(e)}")
        continue

# Sort repositories by commit count in descending order
top_repos.sort(key=lambda x: x[3], reverse=True)

# Print top 10 repositories
print("Repositories with the most commits since {date_input}:")
counter = 1
for repo, latest_commit_id, first_commit_id, commit_count in top_repos:
    print(f"* {counter}: {repo.full_name} (Stars: {repo.stargazers_count}, Commits since {date_input}: {commit_count})")
    print(f" -- First commit on/after {date_input}: {first_commit_id}")
    print(f" -- Latest commit ID: {latest_commit_id}")
    counter += 1