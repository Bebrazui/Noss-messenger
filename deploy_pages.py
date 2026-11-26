import os
import shutil
import subprocess

SRC_DIR = r"C:\Users\ttt79\.gemini\antigravity\scratch\nostr-expressive-messenger\composeApp\build\dist\wasmJs\productionExecutable"
REPO_DIR = r"C:\Users\ttt79\.gemini\antigravity\scratch\nostr-expressive-messenger"
DEPLOY_DIR = r"C:\Users\ttt79\.gemini\antigravity\scratch\gh-pages-deploy"

def remove_readonly(func, path, excinfo):
    import stat
    os.chmod(path, stat.S_IWRITE)
    func(path)

if os.path.exists(DEPLOY_DIR):
    shutil.rmtree(DEPLOY_DIR, onerror=remove_readonly)

# Clone or init branch
subprocess.run(["git", "clone", "--branch", "gh-pages", "https://github.com/Bebrazui/Noss-messenger.git", DEPLOY_DIR], check=True)

# Clean all existing files in DEPLOY_DIR except .git
for item in os.listdir(DEPLOY_DIR):
    if item == ".git":
        continue
    p = os.path.join(DEPLOY_DIR, item)
    if os.path.isdir(p):
        shutil.rmtree(p)
    else:
        os.remove(p)

# Copy everything from SRC_DIR to DEPLOY_DIR
for item in os.listdir(SRC_DIR):
    s = os.path.join(SRC_DIR, item)
    d = os.path.join(DEPLOY_DIR, item)
    if os.path.isdir(s):
        shutil.copytree(s, d)
    else:
        shutil.copy2(s, d)

# Also ensure .nojekyll exists
with open(os.path.join(DEPLOY_DIR, ".nojekyll"), "w") as f:
    f.write("")

# Copy composeResources directly to root as well as fallback
cr_sub = os.path.join(SRC_DIR, "composeResources", "nostr_expressive_messenger.composeapp.generated.resources")
if os.path.exists(cr_sub):
    d_fallback = os.path.join(DEPLOY_DIR, "nostr_expressive_messenger.composeapp.generated.resources")
    if not os.path.exists(d_fallback):
        shutil.copytree(cr_sub, d_fallback)

# Git config, add, commit, push
os.chdir(DEPLOY_DIR)
subprocess.run(["git", "config", "user.name", "Bebrazui"], check=True)
subprocess.run(["git", "config", "user.email", "bebrazui@users.noreply.github.com"], check=True)
subprocess.run(["git", "add", "-A"], check=True)
res = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True)
print("Git status:\n", res.stdout)
if res.stdout.strip():
    subprocess.run(["git", "commit", "-m", "fix: centered responsive onboarding, crisp logo monogram, and fix composeResources path"], check=True)
    subprocess.run(["git", "push", "origin", "gh-pages"], check=True)
    print("DEPLOYED SUCCESSFULLY TO gh-pages!")
else:
    print("No changes to commit.")
