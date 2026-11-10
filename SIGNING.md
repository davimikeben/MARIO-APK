# Android release signing

The pull-request workflow uses `assembleDebug` and never reads release-signing secrets. Signed releases run only for tags matching `v*` and use the protected GitHub Environment `android-release`.

## Create the upload keystore once

On a trusted local machine, generate one long-lived keystore. Enter passwords only at the interactive prompts so they do not enter shell history:

```sh
keytool -genkeypair -v -keystore mario-apk-release.jks -alias marioapk -keyalg RSA -keysize 2048 -validity 10000
```

Back up the keystore in at least two encrypted, access-controlled locations. Do not commit it, attach it to a PR, upload it as a build artifact, or paste it into chat. The repository ignores `.jks` files.

## Configure GitHub Environment secrets

In the repository, open **Settings > Environments > android-release > Environment secrets** and add these secret names with values from the local key setup:

- `ANDROID_KEYSTORE_BASE64` - base64-encoded keystore file
- `ANDROID_KEYSTORE_PASSWORD` - keystore password
- `ANDROID_KEY_ALIAS` - alias used above
- `ANDROID_KEY_PASSWORD` - private-key password

Use Environment secrets, not repository-wide secrets. The workflow decodes the keystore to the ephemeral runner's `$RUNNER_TEMP`, restricts file permissions, and removes it after the job. The private key is not written to the repository or printed in logs. Never send secret values to Replit or include them in an issue or PR.

For GNU/Linux, produce the base64 text locally with `base64 -w 0 mario-apk-release.jks`; on macOS, use `base64 < mario-apk-release.jks | tr -d '\n'`. Paste that output directly into the GitHub Environment secret field and do not save it in the repository.

## Create a release

1. Merge the PR to `main` after review.
2. Create and push a version tag from a commit on `main`, for example:

```sh
git checkout main
git pull --ff-only
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

3. The `android-release` Environment requires manual reviewer approval and allows only `v*` tags. The sole current reviewer, `davimikeben`, can approve their own run; add another trusted collaborator and enable prevention of self-review if independent approval is required.
4. The workflow derives `versionName` from the tag (without leading `v`) and `versionCode` from the release workflow run number. It builds `assembleRelease`, checks ZIP alignment and APK signature, compares the ROM inside the APK byte-for-byte with the repository asset, and uploads only the APK artifact.

The PR workflow remains a debug build with ROM verification and has no access to the signing Environment. Preserve the same keystore for future updates: losing it prevents signing compatible updates for existing installs. The certificate fingerprint printed by `apksigner` is public metadata and can be recorded; the keystore and passwords are not.
