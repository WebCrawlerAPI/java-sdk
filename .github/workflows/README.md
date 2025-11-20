# GitHub Actions Workflows

This directory contains automated CI/CD workflows for the WebCrawlerAPI Standalone Java SDK.

## Workflows

### 1. `test.yml` - Continuous Testing

**Triggers:**
- Push to `main`, `master`, or `develop` branches (when Java/shell files change)
- Pull requests to these branches

**Jobs:**
- **Test on Multiple Java Versions**: Runs tests on Java 8, 11, 17, and 21
- **Compile Examples**: Ensures example code compiles
- **Verify SDK Standalone**: Confirms no external dependencies
- **Summary**: Aggregates results

**Matrix Testing:**
```yaml
java: ['8', '11', '17', '21']
```

### 2. `release.yml` - Release on Semver Tag

**Triggers:**
- Push of semantic version tags **without** 'v' prefix:
  - `1.0.0` ✅
  - `1.2.3` ✅
  - `2.0.0-beta.1` ✅
  - `v1.0.0` ❌ (not triggered)

**Jobs:**

#### Test
- Runs unit tests
- Runs integration tests (if `API_KEY` secret is set)

#### Build
- Compiles SDK
- Creates JAR file: `webcrawlerapi-standalone-{VERSION}.jar`
- Creates source ZIP: `webcrawlerapi-standalone-{VERSION}-src.zip`
- Generates SHA256 checksums

#### Release
- Creates GitHub release
- Uploads artifacts:
  - JAR file
  - Source ZIP
  - SHA256 checksums
- Auto-marks as pre-release if version contains `-` (e.g., `1.0.0-beta`)

#### Notify
- Prints release summary

## Semver Tag Examples

### Valid Tags (will trigger release)

```bash
# Release versions
git tag 1.0.0
git tag 1.2.3
git tag 2.0.0

# Pre-release versions
git tag 1.0.0-alpha
git tag 1.0.0-beta.1
git tag 2.0.0-rc.1

# Push tag to trigger workflow
git push origin 1.0.0
```

### Invalid Tags (will NOT trigger)

```bash
# With 'v' prefix - not supported
git tag v1.0.0  # ❌

# Non-semver formats
git tag release-1.0  # ❌
git tag version1     # ❌
```

## Setup Instructions

### 1. Repository Secrets

For integration tests in workflows, add this secret to your repository:

**Settings → Secrets and variables → Actions → New repository secret**

| Secret Name | Description | Required |
|-------------|-------------|----------|
| `API_KEY` | WebCrawlerAPI key for integration tests | Optional |

### 2. Permissions

The release workflow needs write permissions for creating releases.

**Settings → Actions → General → Workflow permissions**
- Select: "Read and write permissions"
- Enable: "Allow GitHub Actions to create and approve pull requests"

### 3. Branch Protection (Optional)

Protect main branches to require passing tests before merge:

**Settings → Branches → Add branch protection rule**
- Branch name pattern: `main`
- Enable: "Require status checks to pass before merging"
- Select: `Test on Java 8`, `Test on Java 11`, etc.

## Creating a Release

### Step 1: Update Version

Ensure your code is ready for release and all tests pass.

### Step 2: Create and Push Tag

```bash
# Create annotated tag
git tag -a 1.0.0 -m "Release version 1.0.0"

# Or create lightweight tag
git tag 1.0.0

# Push to trigger workflow
git push origin 1.0.0
```

### Step 3: Monitor Workflow

1. Go to **Actions** tab in GitHub
2. Watch "Release on Semver Tag" workflow
3. Wait for all jobs to complete (test → build → release → notify)

### Step 4: Verify Release

Check the **Releases** page:
- Release should be created with tag name
- Artifacts should be attached:
  - `webcrawlerapi-standalone-{VERSION}.jar`
  - `webcrawlerapi-standalone-{VERSION}-src.zip`
  - SHA256 checksums

## Workflow Status Badges

Add these badges to your README:

```markdown
[![Test](https://github.com/YOUR_ORG/YOUR_REPO/actions/workflows/test.yml/badge.svg)](https://github.com/YOUR_ORG/YOUR_REPO/actions/workflows/test.yml)

[![Release](https://github.com/YOUR_ORG/YOUR_REPO/actions/workflows/release.yml/badge.svg)](https://github.com/YOUR_ORG/YOUR_REPO/actions/workflows/release.yml)
```

## Troubleshooting

### Tests Fail in Workflow

**Problem**: Tests pass locally but fail in GitHub Actions

**Solutions**:
- Check Java version compatibility (workflow tests on Java 8, 11, 17, 21)
- Verify file permissions for `.sh` scripts
- Check for hardcoded paths

### Release Not Created

**Problem**: Tag pushed but release workflow didn't run

**Solutions**:
- Ensure tag matches semver pattern (e.g., `1.0.0`, not `v1.0.0`)
- Check workflow file for syntax errors
- Verify repository permissions

### Permission Denied for Release

**Problem**: Workflow fails at "Create Release" step

**Solution**: Enable write permissions:
- Go to **Settings → Actions → General**
- Under "Workflow permissions", select "Read and write permissions"

### Integration Tests Skipped

**Problem**: Integration tests don't run in workflows

**Solution**: Add `API_KEY` secret:
- Go to **Settings → Secrets and variables → Actions**
- Click "New repository secret"
- Name: `API_KEY`, Value: your API key

## Local Testing

Test workflows locally using [act](https://github.com/nektos/act):

```bash
# Install act
brew install act  # macOS
# or
sudo snap install act  # Linux

# Test the test workflow
act push

# Test the release workflow (with specific tag)
act push --eventpath <(echo '{"ref":"refs/tags/1.0.0"}')

# Run specific job
act -j test

# Use specific Java version
act -j test --matrix java:17
```

## Workflow Files

```
.github/
└── workflows/
    ├── README.md        # This file
    ├── test.yml         # Continuous testing
    └── release.yml      # Release automation
```

## Customization

### Change Trigger Patterns

Edit the `on` section in workflow files:

```yaml
on:
  push:
    tags:
      - '[0-9]+.[0-9]+.[0-9]+'  # Semver without v
      - 'v[0-9]+.[0-9]+.[0-9]+' # Add v prefix support
```

### Add More Java Versions

Edit the matrix in `test.yml`:

```yaml
strategy:
  matrix:
    java: ['8', '11', '17', '21', '22']  # Add more versions
```

### Customize Release Notes

Edit the `Extract changelog for version` step in `release.yml`:

```bash
cat > release_notes.md << 'EOF'
# Your custom release notes here
EOF
```

## Best Practices

1. **Always test locally** before pushing tags
2. **Use annotated tags** for better Git history: `git tag -a 1.0.0 -m "message"`
3. **Follow semantic versioning**: MAJOR.MINOR.PATCH
4. **Keep workflows updated** with latest action versions
5. **Monitor workflow runs** for any failures
6. **Add status badges** to README for visibility

## Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Semantic Versioning](https://semver.org/)
- [GitHub Releases](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Action setup-java](https://github.com/actions/setup-java)
- [Action checkout](https://github.com/actions/checkout)

---

Last Updated: 2025-11-19
