# Git Initial Push Fix

## Problem

`git push -u origin main` can fail with:

```text
error: src refspec main does not match any
```

In this project, the actual blocker was earlier in the log: `git add .` failed while trying to index a generated Gradle cache lock file under `caches/9.3.0/fileContent/fileContent.lock`. Because staging failed, no initial commit was created. Because no commit existed on `main`, Git had nothing to push.

## Fix

Run these commands from the project root in Git Bash.

```bash
# 1) Keep accidental generated cache folders out of the repository.
mkdir -p ../_RadiantRush_accidental_cache_backup
for d in caches daemon kotlin-profile native wrapper android jdks notifications; do
  if [ -d "$d" ]; then
    mv "$d" ../_RadiantRush_accidental_cache_backup/
  fi
done

# 2) Reset any partial staging from the failed add attempt.
git reset

# 3) Stage only real project files.
git add .gitignore README.md build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle app docs

# 4) Create the first commit.
git commit -m "Initial Radiant Rush Android app"

# 5) Ensure the branch is named main.
git branch -M main

# 6) Ensure the GitHub remote is correct.
git remote remove origin 2>/dev/null || true
git remote add origin git@github.com:juanc07/Radiant-Rush.git

# 7) Push.
git push -u origin main
```

## Notes

The LF/CRLF warnings are not the main failure. The main failure is the permission-denied cache lock file.

The app source is under `app/`. Root-level folders such as `caches/`, `daemon/`, `native/`, `wrapper/`, `kotlin-profile/`, and `android/` are not part of the Phase 1 app project and should not be committed.

Do not include `.git/` in any ZIP delivery.
