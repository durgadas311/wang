Name "Wang 600 Simulator"
RequestExecutionLevel user
OutFile "wang600apc.exe"
Section "Wang 600 Simulator"
	Var /GLOBAL WANG600HOME
	ExpandEnvStrings $INSTDIR "%HOMEDRIVE%%HOMEPATH%\wang600apc"
	ExpandEnvStrings $WANG600HOME "%HOMEDRIVE%%HOMEPATH%\Wang600Files"
	CreateDirectory $INSTDIR
	CreateDirectory $WANG600HOME
	SetOutPath $INSTDIR
	File "wang600.jar"
	File "Wang600.bat"
	File "Wang614.bat"
	File /r "icons\*.ico"
	File /r "..\common\icons\*.ico"
	CreateShortCut "$DESKTOP\Wang600.lnk" "$INSTDIR\Wang600.bat" "" \
		"$INSTDIR\wang600-48x48.ico" 0 SW_SHOWMINIMIZED
	CreateShortCut "$DESKTOP\Wang614.lnk" "$INSTDIR\Wang614.bat" "" \
		"$INSTDIR\WangX14Edit-128x128.ico" 0 SW_SHOWMINIMIZED
	SetOutPath $WANG600HOME
	File "progs\*.w6*"
	File "progs\*.txt*"
	File "progs\*.wfl*"
	WriteUninstaller $INSTDIR\uninstall.exe
	Exec 'java -cp $INSTDIR\wang600.jar w600initProps'
SectionEnd
Section "Uninstall"
	Delete $DESKTOP\Wang600.lnk
	Delete $DESKTOP\Wang614.lnk
	Delete $INSTDIR\*.jar
	Delete $INSTDIR\*.bat
	Delete $INSTDIR\*.ico
	Delete $INSTDIR\uninstall.exe
	RMDir $INSTDIR
SectionEnd
