site "op+" = orc.lib.math.Add
site "op-" = orc.lib.math.Sub
site "op*" = orc.lib.math.Mult
site "op/" = orc.lib.math.Div
site "op%" = orc.lib.math.Mod
site "opu-" = orc.lib.math.UMinus

site "op<" = orc.lib.comp.Less
site "op<=" = orc.lib.comp.Leq
site "op>" = orc.lib.comp.Greater
site "op>=" = orc.lib.comp.Greq

site "op=" = orc.lib.Equal
site "op/=" = orc.lib.Inequal

site if = orc.lib.bool.If

site "op&&" = orc.lib.bool.And
site "op||" = orc.lib.bool.Or
site "op~" = orc.lib.bool.Not

def ifnot(x) = if(~x) >> false
def Or(a,b)  = let( if(a)   |   if(b)   |  a || b)
def And(a,b) = let(ifnot(a) |  ifnot(b) |  a && b)

site Rtimer = orc.lib.time.Rtimer

site cat = orc.lib.str.Cat
site print = orc.lib.str.Print
site println = orc.lib.str.Println

site localhost = orc.lib.net.Localhost
site SendMail = orc.lib.net.Mail

site Buffer = orc.lib.state.Buffer

{- Temporary fix for boxing problem with proxied Java objects -}
site unbox = orc.runtime.sites.java.Unbox

