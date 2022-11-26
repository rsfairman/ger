% Used to illustrate the two cases of an arc defined by endpoint and radius.

% NEver mind...not worth the trouble.

%input boxes
\input "mymacros.mp"

beginfig(1);


% Need some TeX packages.
  verbatimtex
  %&latex
  \documentclass{article}
  % Allows use of the \bm{} command to get bold math characters.
  \usepackage{bm}
  \begin{document}
  etex






  % Simple end-mill
  draw (0,30)--(0,0)--(5,0)--(5,30);
  label.bot(btex \tiny End-mill etex,(2,-5));

  % Ball-mill
  draw (40,30)--(40,5){down}..(42.5,0){right}..{up}(45,5)--(45,30);
  label.bot(btex \tiny Ball-mill etex,(42,-5));

  draw (158,30)--(158,0){right}..(162.5,5)..{right}(167,0)--(167,30);
  label.bot(btex \tiny Dimpled etex,(162,-5));

  draw (40,-40)--(40,-50){left}..(42,-70){right}..{left}(44,-50)--(44,-40);
  label.bot(btex \tiny Sphere mill etex,(42,-75));

  draw (80,-40)--(80,-55)--(73,-70)--(91,-70)--(84,-55)--(84,-40);
  label.bot(btex \tiny Dovetail etex,(82,-75));


endfig;
end;