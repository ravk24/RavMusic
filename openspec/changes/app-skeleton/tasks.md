## 1. Package rename and dependencies

- [x] 1.1 Rename `namespace` and `applicationId` in `app/build.gradle.kts` to `com.ravk24.ravmusic`, and move the template test sources (`app/src/test`, `app/src/androidTest`) to the matching package directories; verify `.\gradlew.bat assembleDebug` is green and the merged manifest at `app/build/intermediates/merged_manifest/debug/AndroidManifest.xml` shows `package="com.ravk24.ravmusic"`
- [x] 1.2 Add `navigation3 = "1.1.7"` plus `androidx-navigation3-runtime` and `androidx-navigation3-ui` to `gradle/libs.versions.toml`, add both as `implementation` in `app/build.gradle.kts`, and add the Kotlin serialization plugin only if Nav3 `@Serializable` keys require it at compile time; verify the build resolves the artifacts (no "Could not find" in `assembleDebug` output)
- [x] 1.3 Uninstall any previously installed `com.example.ravmusic` debug build from the test device (`adb uninstall com.example.ravmusic`); verify `adb shell pm list packages | grep ravmusic` shows nothing until the new build is installed

## 2. Application, activity, and manifest

- [x] 2.1 Create `RavMusicApp : Application` with an empty `AppContainer`, and declare it as `android:name` in the manifest; verify the app process starts without crashing (`adb logcat` shows no `RuntimeException` on launch)
- [x] 2.2 Create `MainActivity : ComponentActivity` that calls `enableEdgeToEdge()` and sets content to `RavMusicTheme { AppRoot() }`; declare it as the exported `MAIN`/`LAUNCHER` activity; verify the app appears in the launcher and opens
- [x] 2.3 Declare `READ_MEDIA_AUDIO` and `READ_EXTERNAL_STORAGE` (`android:maxSdkVersion="32"`) in the manifest; verify the merged manifest contains both and `grep -c INTERNET` on it returns 0

## 3. Theme

- [x] 3.1 Create `ui/theme/Color.kt` with the mockup palette constants (blurple `#635BFF`, navy `#0A2540`, dark surface `#0C2E4E`, cyan `#80E9FF`, slate `#6B7C93`, light surface `#F6F9FC`, border `#E6EBF1`); verify the file compiles and each constant matches the design canvas hex values
- [x] 3.2 Create `ui/theme/Theme.kt` with `lightColorScheme`/`darkColorScheme` built from those constants and `RavMusicTheme(darkTheme: Boolean = isSystemInDarkTheme(), content)` that never references `dynamicLightColorScheme`/`dynamicDarkColorScheme`; verify with a Compose UI test that `MaterialTheme.colorScheme.primary == Color(0xFF635BFF)` in both light and dark
- [x] 3.3 Create `ui/theme/Type.kt` with a Material 3 `Typography` using the default font family and the mockup's tighter headline letter spacing; verify `@Preview(uiMode = UI_MODE_NIGHT_YES)` and light previews of a sample screen render in Android Studio without errors
- [x] 3.4 Verify edge-to-edge on an API 33+ emulator and an API 26 emulator: status bar icons are dark on the light palette and light on the dark palette, and the bottom navigation bar is fully tappable above the system navigation bar in both gesture and 3-button modes

## 4. Navigation shell

- [x] 4.1 Create `ui/navigation/Routes.kt` with `NavKey` objects `Playlists`, `Folders`, `Settings`; verify they compile and are usable as `rememberNavBackStack` entries
- [x] 4.2 Create `ui/components/AppIcons.kt` with `ImageVector`s for Folder and QueueMusic built from the mockup 24dp path data; verify both render correctly in a `@Preview` at 24dp
- [x] 4.3 Create `ui/playlists/PlaylistsScreen.kt` and `ui/folders/FoldersScreen.kt` as placeholder screens (title header per artboards 1a/1c, "nothing here yet — arrives in a later phase" body); Playlists also has an overflow `MoreVert` menu with a "Settings" item that invokes an `onOpenSettings` lambda; verify each renders in a `@Preview`
- [x] 4.4 Create `ui/settings/SettingsScreen.kt` stub: top bar with title "Settings" and back arrow, body empty, footer "v<versionName> · No INTERNET permission — this app cannot go online."; verify it renders in a `@Preview` and the footer reads the real `versionName`
- [x] 4.5 Create `ui/navigation/AppNavigation.kt`: a `Scaffold` whose `bottomBar` is a `NavigationBar` shown only when the top back-stack entry is `Playlists` or `Folders`, a `NavDisplay` mapping each key to its screen, tab taps that set the stack to `[Playlists]` or `[Playlists, Folders]`, and `Settings` pushed on top; verify Compose UI tests for: cold start shows Playlists selected; tapping Folders shows Folders selected; back from Folders shows Playlists; Settings hides the bottom bar; back from Settings restores the previous tab and bottom bar
- [x] 4.6 Verify tab state retention: a UI test scrolls a placeholder list on Folders, switches to Playlists and back, and asserts the scroll offset survived; and a rotation test (or `StateRestorationTester`) asserts the selected tab survives recreation

## 5. Audio permission gate

- [x] 5.1 Create `permission/AudioPermission.kt` with `audioPermissionFor(sdkInt: Int): String` and sealed `PermissionState { Unknown, Granted, Denied(canRequest: Boolean) }`; verify unit tests: `sdkInt = 33` and `37` return `READ_MEDIA_AUDIO`, `sdkInt = 26` and `32` return `READ_EXTERNAL_STORAGE`
- [x] 5.2 Create `AppViewModel` exposing `StateFlow<PermissionState>`, a `refresh(context)` that checks `checkSelfPermission` + `shouldShowRequestPermissionRationale`, and a `hasRequested` flag in `SavedStateHandle` so permanent denial is only inferred after a request; verify unit tests cover: never requested + not granted → `Denied(canRequest = true)`; requested + no rationale + not granted → `Denied(canRequest = false)`; granted → `Granted`
- [x] 5.3 Create `ui/permission/NoMusicFoundScreen.kt` matching artboard 1h (icon, "No music found", the nothing-leaves-your-phone explanation, "Allow access to audio" button, hint line); when `canRequest == false` the button label changes to "Open settings" and the hint explains access must be enabled in system settings; verify both variants render in `@Preview`s
- [x] 5.4 Create `ui/permission/AudioPermissionGate.kt` that shows `NoMusicFoundScreen` for `Denied`/`Unknown` and `content()` for `Granted`; verify a Compose UI test asserts the placeholder tab content is shown for `Granted` and the "No music found" text for `Denied`
- [x] 5.5 Wire `MainActivity`: `rememberLauncherForActivityResult(RequestPermission())` calling `viewModel.refresh` on result, an `ACTION_APPLICATION_DETAILS_SETTINGS` intent for `Denied(canRequest = false)`, and `LifecycleEventEffect(ON_RESUME) { viewModel.refresh() }`; verify on a device: deny once → dialog reappears on retry; deny permanently → button opens app settings; grant in settings and return → tabs appear without relaunch; revoke in settings and return → empty state appears

## 6. Integration and commit

- [x] 6.1 Run `.\gradlew.bat assembleDebug testDebugUnitTest` and `connectedDebugAndroidTest` against an API 33+ emulator; verify all tasks report `BUILD SUCCESSFUL` and every test passes
- [x] 6.2 Manual walkthrough on an API 33+ device and an API 26 emulator: fresh install → "No music found" → Allow → grant → Playlists placeholder → Folders → back → Playlists → overflow → Settings → back → back exits; rotate on Folders keeps Folders selected; toggle system dark mode while in recents and return; verify each step matches the `app-shell`, `theme`, and `audio-permission` scenarios and note any deviation in this task
- [ ] 6.3 Confirm `git status` shows only intended files, then commit on `main` with a message summarising the skeleton; verify `git log -1` shows the commit and `.\gradlew.bat assembleDebug` from a clean checkout is green
