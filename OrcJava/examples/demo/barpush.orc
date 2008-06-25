-- A push (sequential composition) streaming results of two calls to another call

( David("Write a message for William:") | Adrian("Write a message for William:") ) >x> Email(william, x)


