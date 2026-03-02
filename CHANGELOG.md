## vv10.7.1

- Display pinned app shortcuts (works if default)
- Bump kmpSettingsCore from 0.5.3 to 0.6.1
- Bump androidx.compose.foundation:foundation from 1.10.3 to 1.10.4
- Bump kmpSettingsCore from 0.5.0 to 0.5.3


## vv10.6.4

- Bump kmpSettingsCore from 0.5.3 to 0.6.1
- Bump androidx.compose.foundation:foundation from 1.10.3 to 1.10.4
- Bump kmpSettingsCore from 0.5.0 to 0.5.3


## vv10.6.3

- Use the older widget intent handling (fix #200)
- Bump com.google.devtools.ksp from 2.3.5 to 2.3.6


## vv10.6.2

- Add an option to set app drawer as the home screen (#198)
- bump jvm version to 21
- Bump navigation3Runtime from 1.0.0 to 1.0.1
- Bump androidx.activity:activity-compose from 1.12.3 to 1.12.4
- Bump androidx.compose.foundation:foundation from 1.10.2 to 1.10.3
- Bump androidx.compose:compose-bom from 2026.01.01 to 2026.02.00


## vv10.6.1

- Unify appkeys everywhere (fix #191)


## vv10.5.8

- Fix a crash introduced in prev. releases for multiple entries from same app, due to code reduction
- Test fix colorOS backbutton closing the app on older android versions


## vv10.5.7

- Revert "Try fixing colorOS backbutton closing the app on older android versions"


## vv10.5.6

- Try fixing colorOS backbutton closing the app on older android versions
- Try fixing firefox/fulguris for more than 2 shortcuts
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.3.0 to 2.3.10
- Bump kotlin from 2.3.0 to 2.3.10
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.3.0 to 2.3.10
- Fix duplicate lazycolumn key crash for multiple shortcuts of a single app (#185)


## vv10.5.5

- Fix sort order from prev. releases (#184)
- Bump androidx.work:work-runtime-ktx from 2.11.0 to 2.11.1
- Bump androidx.compose.foundation:foundation from 1.10.1 to 1.10.2
- Bump gradle-wrapper from 9.3.0 to 9.3.1
- Bump androidx.activity:activity-compose from 1.12.2 to 1.12.3
- Bump androidx.compose:compose-bom from 2026.01.00 to 2026.01.01


## vv10.5.4

- Fix sort order from prev. releases (#184)
- Bump androidx.work:work-runtime-ktx from 2.11.0 to 2.11.1
- Bump androidx.compose.foundation:foundation from 1.10.1 to 1.10.2
- Bump gradle-wrapper from 9.3.0 to 9.3.1
- Bump androidx.activity:activity-compose from 1.12.2 to 1.12.3
- Bump androidx.compose:compose-bom from 2026.01.00 to 2026.01.01


## vv10.5.3

- Fix "search in hidden" apps handling (after shortcuts addn) (#177)
- Bump com.google.devtools.ksp from 2.3.4 to 2.3.5
- Bump gradle-wrapper from 9.2.1 to 9.3.0


## vv10.5.2

- Force status bar to hide (for compat with older android systems, #173)


## vv10.5.1

- Add shortcut handling support (#172)
- Bump org.jetbrains.kotlinx:kotlinx-serialization-json


## vv10.4.2

- System theme change affects the status bar, and add a setting to auto update wallpaper (fix #169)
- Upgrade to agp 9.0.0
- Bump androidx.compose:compose-bom from 2025.12.01 to 2026.01.00
- Bump androidx.compose.foundation:foundation from 1.10.0 to 1.10.1
- Bump kmpSettingsCore from 0.4.3 to 0.5.0
- Add an option to disable long press
- Misc. duplicates reduction
- Do not auto open first match when selecting swipe app
- Bump kmpSettingsCore from 0.4.2 to 0.4.3


## vv10.4.1

- System theme change affects the status bar, and add a setting to auto update wallpaper (fix #169)
- Upgrade to agp 9.0.0
- Bump androidx.compose:compose-bom from 2025.12.01 to 2026.01.00
- Bump androidx.compose.foundation:foundation from 1.10.0 to 1.10.1
- Bump kmpSettingsCore from 0.4.3 to 0.5.0
- Add an option to disable long press
- Misc. duplicates reduction
- Do not auto open first match when selecting swipe app
- Bump kmpSettingsCore from 0.4.2 to 0.4.3


## vv10.3.4

- Add toggle to hide "Search Web" (req)


## vv10.3.3

- Fix swipe app selection (#163)


## vv10.3.2

- Fix app drawer not opening the keyboard on app exit on nav 3
- Add an option to return to home after closing an app
- Add "exact match" search type
- Check if it can resized based on the existing page's items only
- remove unneeded tasks in yml and kts


## vv10.3.1

- Fix app drawer not opening the keyboard on app exit on nav 3
- Add an option to return to home after closing an app
- Add "exact match" search type
- Check if it can resized based on the existing page's items only
- remove unneeded tasks in yml and kts


## vv10.2.2

- Add an option to hide the page indicators
- Bump kmpSettingsCore from 0.3.3 to 0.4.2


## vv10.2.1

- Add home pages support (#161)


## vv10.1.1

- Add Import/Export (#160)
- Revert "Delete clauncher_icon.svg"
- Delete clauncher_icon.svg


## vv10.0.3

- Add consumer-rules to settings-core, and remove proguard rules
- Add consumer-rules to settings-core, and remove proguard rules
- Fix setting names not being visible


## vv10.0.2

- Bump com.google.devtools.ksp from 2.3.0 to 2.3.4
- Reduce icon size, fix wallpaper not appearing
- bump major ver
- remove old unused deps
- Navigation 3 + Snackbars
- Migrate to my settings module, and use koin for deps
- Add Dpad support
- Fix broken icon files, and improve gradle.kts (use my dist plugin)
- Improve icon pack handling
- resolve user strings to prevent misc. profile apps from not working on home screen
- Do not use null class names
- Use same appKey format everywhere
- Use gradle 9
- Use a proper theme color for "No apps found for ..." text
- Delete old unused xml files, and disable jetifier
- Bump androidx.activity:activity-compose from 1.12.1 to 1.12.2
- Update full_description.txt to add line breaks (#155)
- Update short_description.txt
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.21 to 2.3.0
- Bump plugin.serialization from 2.2.21 to 2.3.0
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.21 to 2.3.0
- Bump kotlin from 2.2.21 to 2.3.0
- Bump actions/upload-artifact from 5 to 6 in /.github/workflows


## vv10.0.1

- Bump com.google.devtools.ksp from 2.3.0 to 2.3.4
- Reduce icon size, fix wallpaper not appearing
- bump major ver
- remove old unused deps
- Navigation 3 + Snackbars
- Migrate to my settings module, and use koin for deps
- Add Dpad support
- Fix broken icon files, and improve gradle.kts (use my dist plugin)
- Improve icon pack handling
- resolve user strings to prevent misc. profile apps from not working on home screen
- Do not use null class names
- Use same appKey format everywhere
- Use gradle 9
- Use a proper theme color for "No apps found for ..." text
- Delete old unused xml files, and disable jetifier
- Bump androidx.activity:activity-compose from 1.12.1 to 1.12.2
- Update full_description.txt to add line breaks (#155)
- Update short_description.txt
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.21 to 2.3.0
- Bump plugin.serialization from 2.2.21 to 2.3.0
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.21 to 2.3.0
- Bump kotlin from 2.2.21 to 2.3.0
- Bump actions/upload-artifact from 5 to 6 in /.github/workflows


## vv9.10.9

- Add a setting to show names after typing x chars
- Bump androidx.activity:activity-compose from 1.12.0 to 1.12.1
- Bump androidx.activity:activity-compose from 1.11.0 to 1.12.0
- Bump androidx.datastore:datastore-preferences-core from 1.1.7 to 1.2.0
- Bump actions/checkout from 5 to 6 in /.github/workflows
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.4 to 2.10.0
- Bump androidx.datastore:datastore-preferences from 1.1.7 to 1.2.0
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.4 to 2.10.0


## vv9.10.8

- Remove relevance ranking (sort orders were better (with LastLaunch, not really relevant too...)
- Bump navigationFragmentKtx from 2.9.5 to 2.9.6
- Bump actions/upload-artifact from 4 to 5 in /.github/workflows
- Improve the cclauncher mono icon
- retry with few changes
- Add accessibility dialog as suggested (may need to re-enable if using the setting beforehand)


## vv9.10.7

- Bump actions/upload-artifact from 4 to 5 in /.github/workflows
- Improve the cclauncher mono icon
- retry with few changes
- Add accessibility dialog as suggested (may need to re-enable if using the setting beforehand)


## vv9.10.5

- Bump actions/upload-artifact from 4 to 5 in /.github/workflows
- Improve the cclauncher mono icon
- retry with few changes
- Add accessibility dialog as suggested (may need to re-enable if using the setting beforehand)


## vv9.10.4

- retry with few changes
- Add accessibility dialog as suggested (may need to re-enable if using the setting beforehand)


## vv9.10.3

- disable gplay upload
- prefer shorter names when searching; refresh app list for certain changes automatically; add home app label alignment (fix #134, #135, #133)
- add mono icon


## vv9.10.2

- [chore] Update build.gradle.kts
- Bump plugin.serialization from 2.2.20 to 2.2.21
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.20 to 2.2.21
- Bump kotlin from 2.2.20 to 2.2.21
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.20 to 2.2.21
- Bump androidx.work:work-runtime-ktx from 2.10.5 to 2.11.0
- Revert versionCode and versionName in build.gradle.kts
- Delete fastlane/metadata/android/en-US/changelogs/890.txt


## vv9.10.1

- [chore] Update build.gradle.kts
- Bump plugin.serialization from 2.2.20 to 2.2.21
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.20 to 2.2.21
- Bump kotlin from 2.2.20 to 2.2.21
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.20 to 2.2.21
- Bump androidx.work:work-runtime-ktx from 2.10.5 to 2.11.0
- Revert versionCode and versionName in build.gradle.kts
- Delete fastlane/metadata/android/en-US/changelogs/890.txt


## vv9.9.9

- Change default Google Play track to 'production'
- Bump gradle/actions from 4 to 5 in /.github/workflows


## vv9.9.8

- Change default Google Play track to 'production'
- Bump gradle/actions from 4 to 5 in /.github/workflows


## vv9.9.7

- Add gesture senstivity slider (fix #125)
- Suppress a warn
- Bump androidx.compose.material3:material3-android from 1.3.2 to 1.4.0
- Update 860.txt
- Bump androidx.work:work-runtime-ktx from 2.10.4 to 2.10.5
- Bump navigationFragmentKtx from 2.9.4 to 2.9.5


## vv9.9.6

- Update android.yml to continue even if aab upload fails
- Add text color picker (for lighter backgrounds)
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.3 to 2.9.4
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.3 to 2.9.4
- Bump com.google.code.gson:gson from 2.13.1 to 2.13.2
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.10 to 2.2.20
- Bump kotlin from 2.2.10 to 2.2.20
- Bump androidx.activity:activity-compose from 1.10.1 to 1.11.0
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.10 to 2.2.20
- Bump navigationFragmentKtx from 2.9.3 to 2.9.4
- Bump plugin.serialization from 2.2.10 to 2.2.20
- Bump androidx.work:work-runtime-ktx from 2.10.3 to 2.10.4
- Update android.yml for GH release notes
- add missed id
- Update android.yml
- fix android.yml
- Pin workflow commit to prevent repro mismatches
- Bump com.google.android.material:material from 1.12.0 to 1.13.0
- rm duplicate code
- Update build.gradle.kts
- Update build.gradle.kts
- Update android.yml for universal apk
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.2 to 2.9.3
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.2 to 2.9.3


## vv9.9.5

- Add text color picker (for lighter backgrounds)
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.3 to 2.9.4
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.3 to 2.9.4
- Bump com.google.code.gson:gson from 2.13.1 to 2.13.2
- Bump org.jetbrains.kotlin:kotlin-reflect from 2.2.10 to 2.2.20
- Bump kotlin from 2.2.10 to 2.2.20
- Bump androidx.activity:activity-compose from 1.10.1 to 1.11.0
- Bump org.jetbrains.kotlin:kotlin-stdlib from 2.2.10 to 2.2.20
- Bump navigationFragmentKtx from 2.9.3 to 2.9.4
- Bump plugin.serialization from 2.2.10 to 2.2.20
- Bump androidx.work:work-runtime-ktx from 2.10.3 to 2.10.4
- Update android.yml for GH release notes
- add missed id
- Update android.yml
- fix android.yml
- Pin workflow commit to prevent repro mismatches
- Bump com.google.android.material:material from 1.12.0 to 1.13.0
- rm duplicate code
- Update build.gradle.kts
- Update build.gradle.kts
- Update android.yml for universal apk
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.2 to 2.9.3
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.2 to 2.9.3


## vv9.9.4

- Update build.gradle.kts
- Update build.gradle.kts
- Update android.yml for universal apk
- Bump androidx.lifecycle:lifecycle-viewmodel-compose from 2.9.2 to 2.9.3
- Bump androidx.lifecycle:lifecycle-viewmodel-ktx from 2.9.2 to 2.9.3


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

