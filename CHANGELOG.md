## vv9.9.3

- Add tap to open setting to fix #104
- Bump actions/setup-java from 4 to 5 in /.github/workflows
- Add transliteration setting separated (niche)
- Bump com.android.application from 8.12.0 to 8.12.1
- Bump gradle/actions from 3 to 4 in /.github/workflows
- Fix all abis being built even if target abi is specified
- Update 820.txt
- Update CHANGELOG.md
- Update android.yml
- Bump actions/checkout from 4 to 5 in /.github/workflows
- Bump gradle/gradle-build-action from 2 to 3 in /.github/workflows
- Update dependabot.yml (fix spell)
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.0 to 2.2.10
- Bump kotlin from 2.2.0 to 2.2.10
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.0 to 2.2.10
- Bump plugin.serialization from 2.2.0 to 2.2.10
- Bump androidx.core:core-ktx from 1.16.0 to 1.17.0
- Bump com.android.application from 8.11.1 to 8.12.0


## vv9.9.2

- downgrade  AGP to 8.11.1
- rm more legacy home apps code
- Separate out settings components (refactor)
- Group all permission related fns in PermissionManager
- Standardize animation access and rm old files
- Reuse LauncherListItem for hidden apps and normal drawer


## vv9.9.1

- Update build.gradle.kts to increment x.ver.x
- Major settings refactor and maintainability improvements
- Minor code reduction
- Use base dialogs for resizing, works better
- misc toast code reduction
- Remove deprecated homeapp handling logic (cont.)
- Remove deprecated homeapp handling
- update dep
- Unify showToast
- move perms related fns to permission manager
- Reduce duplication of createFromBitmap fn
- Small constants refactoring
- rm unnecessary check
- Update dependabot-auto-merge.yml
- fix: update plain background according to app's theme


## vv9.8.1

- Bump ver
- Add screen orientation options from #83 and remove other redundant checks
- update gradle to 8.14.3


## vv9.7.3

- fix #81 (non-performant)
- disable font weight when custom font is used


## vv9.7.2

- Add more context for private space, add font selection from file from #79


## vv9.7.1

- Add refresh trigger for settings screen
- Add some for logging to check later, used wrong name (cp)
- Move towards private space implementation (experimental)
- Update AppDrawerScreen.kt to use delay instead
- Try fixing recents first keyboard bug


## vv9.6.7

- Update AppDrawerScreen.kt to use delay instead
- Try fixing recents first keyboard bug


## vv9.6.6

- Autoscroll to first element on list change (only in recent apps sort order) #76


## vv9.6.5

- Add suggestion from #76


## vv9.6.4

- Update android.yml


# Changelog

## vv9.6.3

- Try fixing the unresponsive movement of widget issue, fix HomeApp movement
- Update android.yml
- add etc donation method
- Update full_description.txt
- minimize android.yml contents

