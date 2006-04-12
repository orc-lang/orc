-- mail parameters:
--    from
--    to (can be tuple: use let(addr1, addr2)
--    subject
--    content
--    SMTP server
SendMail("misra@cs.utexas.edu", 
		 "wcook@cs.utexas.edu",
		 "this is a test", 
		 "I think it is working now", 
		 "mail.cs.utexas.edu")

SendMail("wcook@cs.utexas.edu", 
		 let("misra@cs.utexas.edu", "wcook@cs.utexas.edu"),
		 "this is a test", 
		 "I think it is working now", 
		 "mail.cs.utexas.edu")
		 