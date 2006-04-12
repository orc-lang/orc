
files=`cat <<EOT
src/orc/Orc.java
src/orc/runtime/OrcEngine.java
src/orc/runtime/Token.java
src/orc/runtime/Environment.java
src/orc/runtime/nodes/Node.java
src/orc/runtime/nodes/Fork.java
src/orc/runtime/nodes/Assign.java
src/orc/runtime/nodes/Where.java
src/orc/runtime/nodes/Store.java
src/orc/runtime/nodes/Define.java
src/orc/runtime/nodes/Call.java
src/orc/runtime/nodes/Return.java
src/orc/runtime/nodes/Param.java
src/orc/runtime/nodes/Literal.java
src/orc/runtime/nodes/Variable.java
src/orc/runtime/values/Value.java
src/orc/runtime/values/BaseValue.java
src/orc/runtime/values/Constant.java
src/orc/runtime/values/Tuple.java
src/orc/runtime/values/GroupCell.java
src/orc/runtime/values/Callable.java
src/orc/runtime/values/Closure.java
src/orc/runtime/sites/Site.java
src/orc/runtime/sites/Let.java
src/orc/runtime/sites/Calc.java
src/orc/runtime/sites/Rtimer.java
src/orc/runtime/sites/Mail.java
src/orc/parser/OrcParser.g
src/orc/ast/OrcProcess.java
src/orc/ast/ParallelComposition.java
src/orc/ast/SequentialComposition.java
src/orc/ast/AsymmetricParallelComposition.java
src/orc/ast/Define.java
src/orc/ast/Call.java
src/orc/ast/Literal.java
src/orc/ast/Variable.java
`

# -W for output format
# enscript -Ejava --no-formfeed -f "Courier@8" -2r -o orc.ps src/orc/parser/OrcParser.g `find . -name *.java -print | grep -v parser.*java` 
a2ps  -r -f 8 -T 4 --file-align virtual -Ejava -o orc.ps $files
ps2pdf orc.ps
rm orc.ps

