; Velofine installer script. AppVersion/JpackageOutputDir/OutputDir are normally passed in via
; /D flags from the :installer:innoSetupCompile Gradle task; the #ifndef fallbacks below let this
; also compile standalone from the Inno Setup IDE for manual iteration.
#ifndef AppVersion
  #define AppVersion "0.1.0"
#endif
#ifndef JpackageOutputDir
  #define JpackageOutputDir "build\jpackage"
#endif
#ifndef OutputDir
  #define OutputDir "build\innosetup"
#endif

[Setup]
AppId={{542265C3-3327-4BE6-B5DB-5A273983B216}
AppName=Velofine
AppVersion={#AppVersion}
AppPublisher=siliconcode-dev
AppPublisherURL=https://github.com/siliconcode-dev/velofine
DefaultDirName={localappdata}\Velofine
DefaultGroupName=Velofine
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir={#OutputDir}
OutputBaseFilename=Velofine-Setup
SetupIconFile=branding\velofine.ico
UninstallDisplayIcon={app}\Velofine.exe
WizardStyle=dark
WizardImageFile=branding\wizard-image.bmp
WizardSmallImageFile=branding\wizard-small.bmp
WizardImageStretch=no
WizardBackColor=clBlack
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Messages]
WelcomeLabel2=This will install [name/ver] on your computer.%n%nNote: Velofine is unsigned, open-source software (no paid code-signing certificate for v1) — Windows SmartScreen may show an "unrecognized publisher" warning when you first run this installer or launch Velofine. This is expected and documented in the README; it is not a sign of malware.%n%nIt is recommended that you close Minecraft and the Minecraft Launcher before continuing.

[Files]
Source: "{#JpackageOutputDir}\Velofine\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{group}\Velofine"; Filename: "{app}\Velofine.exe"
Name: "{group}\Uninstall Velofine"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\Velofine.exe"; Parameters: "--install-profile ""{code:GetMinecraftDir}"""; StatusMsg: "Generating Velofine launcher profile..."; Flags: runhidden waituntilterminated

[UninstallRun]
Filename: "{app}\Velofine.exe"; Parameters: "--uninstall-profile ""{code:GetSavedMinecraftDir}"""; RunOnceId: "RemoveVelofineProfile"; Flags: runhidden waituntilterminated

[Code]
var
  MinecraftDirPage: TInputDirWizardPage;

function DefaultMinecraftDir(): String;
begin
  Result := ExpandConstant('{userappdata}\.minecraft');
end;

procedure InitializeWizard;
begin
  MinecraftDirPage := CreateInputDirPage(wpSelectDir,
    'Select Minecraft Directory', 'Where is your .minecraft folder?',
    'Velofine installs alongside your existing vanilla Minecraft installation as a separate ' +
    'launcher profile — it does not modify vanilla files. Select your .minecraft folder below. ' +
    'Vanilla Minecraft must already be installed and have been launched at least once.',
    False, '');
  MinecraftDirPage.Add('');
  MinecraftDirPage.Values[0] := DefaultMinecraftDir();
end;

function GetMinecraftDir(Param: String): String;
begin
  Result := MinecraftDirPage.Values[0];
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep = ssPostInstall then
    SaveStringToFile(ExpandConstant('{app}\minecraft-dir.txt'), MinecraftDirPage.Values[0], False);
end;

function GetSavedMinecraftDir(Param: String): String;
var
  Contents: AnsiString;
begin
  if LoadStringFromFile(ExpandConstant('{app}\minecraft-dir.txt'), Contents) then
    Result := String(Contents)
  else
    Result := DefaultMinecraftDir();
end;
