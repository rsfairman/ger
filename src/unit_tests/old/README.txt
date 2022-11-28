
Feed these through the program. Ultimately, they should all be run through
the complete interpreter, with all of the layers, but it makes sense to
start testing by feeding each file to the layer that it was designed to
test. Also, it may not make sense to run every one of these files through
the complete interpreter. For instance, the lexer.txt and parser.txt files
are full of all kinds of dumb lexical and syntax errors, and the only thing
that's really of interest at the top layer is whether these errors are
properly propagated upward. So, I should be able to run these files through
the complete interpreter, with all of the layers, and if there's a problem,
then I should back up and look at the layers one at a time.

Another reason to be choosy about which layer is given which test file is
that I was not very careful in choosing the radius or I/J/K values for
G02/03 commands. Many of the inputs I chose are probably not valid arcs
of circles, although they do test the layer of interest in the way I
want to do.

The way to see the output is with the various choices in the "Test" menu,
which will not appear in the final version of the program. The output appears
on stdout -- i.e., qDebug().

There are a couple of goals here. Obviously, I want the output to make sense
when there is no error in the input. I also want warnings and errors to appear 
as such, and for the interpreter to do its best to make headway in the presence
of errors. Except in unusual situations, the program should not halt at the
first error it sees.

These tests are not intended as tests of the interpreter as such, but only
as tests of the translation aspect of the interpreter process. The interpreter
converts G-code to a subset of the entire universe of G-codes (basically to
G00, G01, G02, G03) and the input files with names like lexer.txt, parser.txt,
layer00.txt, etc., are to test whether the program properly translates these
input files to the G-code subset. Testing the conversion of G-code to pulses
is a different question.
