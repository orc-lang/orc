@ECHO OFF
ASSOC .orc=OrcScript
FTYPE OrcScript=javaw -jar "%CD%\@orc.jar@" "%%1"
