# Release Guide

Quick reference for creating releases of the WebCrawlerAPI Standalone Java SDK.

## Prerequisites

✅ All tests passing locally
✅ Code reviewed and approved
✅ Documentation updated
✅ CHANGELOG updated (if applicable)

## Release Process

### 1. Choose Version Number

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** (1.0.0 → 2.0.0): Breaking changes
- **MINOR** (1.0.0 → 1.1.0): New features, backwards compatible
- **PATCH** (1.0.0 → 1.0.1): Bug fixes, backwards compatible

Pre-release formats:
- `1.0.0-alpha` - Alpha release
- `1.0.0-beta.1` - Beta release (numbered)
- `1.0.0-rc.1` - Release candidate

### 2. Create Tag

```bash
# Replace X.Y.Z with your version
VERSION="1.0.0"

# Create annotated tag (recommended)
git tag -a $VERSION -m "Release version $VERSION"

# Verify tag
git tag -l $VERSION
git show $VERSION
```

### 3. Push Tag

```bash
# Push the tag (this triggers the release workflow)
git push origin $VERSION
```

**⚠️ Important**: Do NOT use 'v' prefix (e.g., `v1.0.0`). Use `1.0.0` instead.

### 4. Monitor Workflow

1. Go to: https://github.com/YOUR_ORG/YOUR_REPO/actions
2. Click on "Release on Semver Tag" workflow
3. Watch the progress:
   - ✅ Test (runs unit and integration tests)
   - ✅ Build (creates JAR and source ZIP)
   - ✅ Release (creates GitHub release)
   - ✅ Notify (prints summary)

### 5. Verify Release

Check: https://github.com/YOUR_ORG/YOUR_REPO/releases

Verify the release contains:
- Release notes
- JAR file: `webcrawlerapi-standalone-{VERSION}.jar`
- Source ZIP: `webcrawlerapi-standalone-{VERSION}-src.zip`
- SHA256 checksums for both files

### 6. Announce (Optional)

- Update README badges
- Announce on social media
- Update documentation site
- Notify users

## Quick Commands

```bash
# Complete release in one go
VERSION="1.0.0" && \
git tag -a $VERSION -m "Release version $VERSION" && \
git push origin $VERSION && \
echo "✅ Release $VERSION triggered!"
```

## Pre-release Example

```bash
# Create beta release
git tag -a 1.0.0-beta.1 -m "Beta release 1.0.0-beta.1"
git push origin 1.0.0-beta.1

# This will be marked as "pre-release" in GitHub
```

## Hotfix Release Example

```bash
# Quick patch for urgent fix
git checkout main
git pull origin main

# Make your fix
# ... edit files ...

# Commit fix
git add .
git commit -m "Fix critical bug in scrape method"

# Create patch release
git tag -a 1.0.1 -m "Hotfix: Fix critical bug in scrape method"
git push origin main
git push origin 1.0.1
```

## Delete/Redo Release

If you need to delete and recreate a release:

```bash
# Delete remote tag
git push origin --delete 1.0.0

# Delete local tag
git tag -d 1.0.0

# Delete GitHub release manually:
# Go to Releases → Click release → Delete release

# Then create new tag
git tag -a 1.0.0 -m "Release version 1.0.0"
git push origin 1.0.0
```

## Troubleshooting

### Tag pushed but no workflow

**Check**:
- Tag format is correct (no 'v' prefix)
- Tag matches pattern: `[0-9]+.[0-9]+.[0-9]+`
- Workflow file exists in `.github/workflows/release.yml`

**Fix**:
```bash
# View workflow file
cat .github/workflows/release.yml | grep "tags:"
```

### Workflow fails at test step

**Check**:
- Tests pass locally: `cd tests && ./run-tests.sh`
- Java version compatibility

**Fix**:
- Run tests locally first
- Check workflow logs for details

### Release creation fails

**Check**:
- Repository has write permissions enabled
- GITHUB_TOKEN has sufficient permissions

**Fix**:
- Go to Settings → Actions → General
- Select "Read and write permissions"

### Artifacts not uploading

**Check**:
- Build step completed successfully
- Files exist in `build/` directory

**Fix**:
- Check build logs
- Verify file paths in workflow

## Version History Example

```
2.0.0 - 2025-02-01 - Major refactor with breaking changes
1.2.0 - 2025-01-15 - Add async polling support
1.1.1 - 2025-01-10 - Fix JSON parsing bug
1.1.0 - 2025-01-05 - Add integration tests
1.0.1 - 2025-01-02 - Documentation updates
1.0.0 - 2025-01-01 - Initial release
```

## Checklist

Before pushing tag:

- [ ] All tests pass locally
- [ ] Version number chosen (semver)
- [ ] Code changes committed
- [ ] Documentation updated
- [ ] README updated if needed
- [ ] No 'v' prefix in tag
- [ ] Tag created locally
- [ ] Ready to push

After workflow completes:

- [ ] Workflow succeeded (all green)
- [ ] Release created on GitHub
- [ ] Artifacts uploaded (JAR, ZIP, checksums)
- [ ] Release notes look correct
- [ ] Downloads work correctly

## CI/CD Pipeline

```
Tag Push → Test → Build → Release → Notify
   ↓         ↓       ↓        ↓         ↓
  1.0.0   Unit    JAR+ZIP  GitHub   Summary
           Int              Release
```

## Resources

- [Semantic Versioning](https://semver.org/)
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)
- [Workflow Documentation](.github/workflows/README.md)

---

**Remember**: Always test locally before creating a release tag!
