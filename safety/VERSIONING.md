# PRD Versioning Setup Guide

## Files Updated
1. **pom.xml** - Updated with maven-release-plugin and versioning config
2. **version.properties** - New file to track version info
3. **application.properties** - Configured to load version info

## Current Version
- **Development**: 1.0.0-SNAPSHOT (in dev branch)
- **Production**: 1.0.0 (in prd branch after release)

## Branch Strategy

### Step 1: Create Development & Production Branches
```bash
# Switch to dev branch for development
cd d:\Test_share\HSE2\safety
git checkout -b dev
git push -u origin dev

# Create prd branch from main (for production releases)
git checkout main
git checkout -b prd
git push -u origin prd
```

### Step 2: Daily Workflow
```bash
# For daily development: work on dev branch
git checkout dev
# Make changes and commit

# When ready to release to PRD: 
git checkout prd
git merge --no-ff dev   # Merge with merge commit for history
git push origin prd
```

## Releasing to Production

### Option A: Automatic Release (Recommended)
```bash
# From prd branch
git checkout prd

# Prepare and perform release
mvn release:prepare release:perform

# This will:
# 1. Verify no uncommitted changes
# 2. Change version from 1.0.0-SNAPSHOT to 1.0.0
# 3. Commit changes
# 4. Create git tag v1.0.0
# 5. Update to next dev version 1.0.1-SNAPSHOT
# 6. Push to remote
```

### Option B: Manual Release
```bash
# From prd branch
git checkout prd

# Step 1: Update version in pom.xml
# Change: <version>1.0.0-SNAPSHOT</version>
# To:     <version>1.0.0</version>
mvn versions:set -DnewVersion=1.0.0

# Step 2: Commit
git add pom.xml
git commit -m "chore: release version 1.0.0"

# Step 3: Tag the release
git tag -a v1.0.0 -m "Production Release 1.0.0"

# Step 4: Push
git push origin prd
git push origin v1.0.0

# Step 5: Update to next dev version
mvn versions:set -DnewVersion=1.0.1-SNAPSHOT
git add pom.xml
git commit -m "chore: bump version to 1.0.1-SNAPSHOT"
```

## Building for Different Environments

### Development Build
```bash
git checkout dev
mvn clean package -DskipTests -Dspring.profiles.active=dev
```

### Production Build
```bash
git checkout prd
mvn clean package -DskipTests -Dspring.profiles.active=prd
```

## Version Information Locations

### In Java Code
```java
@Value("${app.version}")
private String appVersion;

@Value("${app.build.timestamp}")
private String buildTime;

@Value("${app.name}")
private String appName;
```

### In HTML Template
```html
<p>Version: <th:text="${@environment.getProperty('app.version')}"></th:text></p>
<p>Build: <th:text="${@environment.getProperty('app.build.timestamp')}"></th:text></p>
```

### In properties.properties
All version info is loaded from **version.properties** which is populated during Maven build:
- `app.name` - HSE Safety Management System
- `app.version` - 1.0.0 (from pom.xml)
- `app.build.timestamp` - Build date/time
- `app.build.java.version` - Java version used

## Git Branch Protection (Optional but Recommended)

For GitHub/GitLab - protect prd branch:
- Require pull request reviews before merge
- Dismiss stale pull request approvals
- Require branches to be up to date before merging
- Require status checks to pass before merging

## Release Checklist

Before releasing to PRD:
- [ ] All tests pass: `mvn clean test`
- [ ] No uncommitted changes: `git status`
- [ ] Dev branch is up to date with all features
- [ ] Update CHANGELOG.md with release notes
- [ ] Tag format: v1.0.0 (semantic versioning)
- [ ] Verify environment-specific configs are correct

