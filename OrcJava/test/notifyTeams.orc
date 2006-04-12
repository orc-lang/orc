def foo(from, to, subject, body, server) =
	println(from,":", to,":", subject,":", body,":", server)
   	>> SendMail(from, to, subject, body, server)
	
def notify(name1, m1, m2, name2) =
	foo("wcook@cs.utexas.edu", 
		 let(m1, m2,"wcook@cs.utexas.edu"),
			"CS345 Project 3 Teams", 
		 cat("I'm resending these email with **names**. I'm sorry about the duplicates. The emails of your team, ", name1, " and ", name2, " are:\n\n", 
		 	  m1,"\n", m2, 
"\n\nPlease contact each other to work on the project",
"\n\nNOTE: This message sent with our experimental programming language, Orc. See http://www.cs.utexas.edu/users/wcook/projects/orc/ for more information.\n"),
"mail.cs.utexas.edu")

  notify("BOOTHE, PAUL ANDREW","boothepa@mail.utexas.edu","ariel.apostoli@mail.utexas.edu","APOSTOLI, ARIEL")
| notify("MADIGAN, RYAN MICHAEL","rmadigan@mail.utexas.edu","alut266@hotmail.com","DAM, ALBERT DANG")
| notify("YU, YONGSHIN","yongshinyu@mail.utexas.edu","bdillon@mail.utexas.edu","DILLON, CHARLES BRANDON")
| notify("MAJOR, DAVID","utmail@davidmajor.com","digital_joyce@yahoo.com","KUNG, JOYCE CHO YUET")
| notify("BASS, GARETT THOMAS","garettbass@studiotekne.com","lanroche@mail.utexas.edu","ROCHE, DAVID A. II")
| notify("WU, MINHAO","themaelstrom@mail.utexas.edu","davidho@mylinuxisp.com","HO, DAVID W.")
| notify("KRETSCHMAR, ANDREW THOMAS","atkretsch@mail.utexas.edu","ericpurwaka@mail.utexas.edu","PURWAKA, ERIC")
| notify("HIGHLAND, TREVOR SCOTT","trevor_highland@mail.utexas.edu","ericpwei@mail.utexas.edu","WEI, ERIC PAUL")
| notify("CARABIAS-ORTI, JULIO JOSE","CARABIASJULIO@HOTMAIL.COM","franmoyang@mail.utexas.edu","MOYA-ANGULO, FRANCISCO J.")
| notify("WANG, MICHAEL KAE","mkwang55@mail.utexas.edu","jagoldberg@mail.utexas.edu","GOLDBERG, JASON AARON")
| notify("MAROCHA, VISHAL","vmarocha@hotmail.com","jpnance@mail.utexas.edu","NANCE, JAMES PATRICK")
| notify("SHANMUGAM, PERUMAAL S.","perumaal@mail.utexas.edu","korystrickland@gmail.com","STRICKLAND, KORY A.")
| notify("DE LUNA, TANIS JR.","crono@mail.utexas.edu","pwilly@cs.utexas.edu","WILLIAMS, PAUL LEIGHTON")
| notify("KENT, STEPHEN WARREN","stephenkent@mail.utexas.edu","jsneden@mail.utexas.edu","SNEDEN, JEFF CLARK")






























