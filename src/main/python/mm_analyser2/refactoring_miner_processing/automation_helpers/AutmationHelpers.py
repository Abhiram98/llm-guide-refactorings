import git
import os

class EmmHelper:
    def __init__(self, include_static=False):
        self.directory = "filter_emm"
        self.outdir = "mm-assist-emm"
        self.include_static = include_static
    def get_parent_commit(self, ref):
        if ref['extraction_results']['success']:
            return ref['extraction_results']['newCommitHash']
        return None

class MmHelper:
    def __init__(self, project_path):
        self.project_path = project_path
        self.repo = git.Repo(project_path)
        self.directory = "filter_fp"
        self.outdir = "mm-assist"

    def get_parent_commit(self, ref):
        parent_commit = self.repo.commit(ref['sha1']).parents[0].hexsha
        return parent_commit
