@ECHO OFF
ASSOC .orc=OrcScript
FTYPE OrcScript=javaw -cp "%CD%\@orc.jar@:%CD%\lib/\*" orc.Main "%%1"
