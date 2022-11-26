% Different tool shapes

input boxes

beginfig(1);


% Need some TeX packages.
  verbatimtex
  %&latex
  \documentclass{article}
  % Allows use of the \bm{} command to get bold math characters.
  \usepackage{bm}
  \begin{document}
  etex


  % This is a little folder-like icon.
  picture folder;
  
  % What I really want is slightly rounded edged, but that's not so easy.
  tabheight = 3;
  tabwidth = 8;
  pair offset;
  offset = (1,1);
  z0 = (0,14);
  z1 = (20,0);
  
  % The basic rectangle
  draw (0,0)--z1--z0+z1--z0--cycle;
  % The upper left corner.
  draw z0--z0+offset--(xpart offset,ypart z0);
  % The upper right corner, which goes down to the lower right corner too.
  draw z0+z1--z0+z1+offset--z1;
  % The tab at the top.
  draw z0+offset--z0+offset shifted (0,tabheight)--z0+offset shifted(tabwidth,tabheight)--z0+offset shifted (tabwidth,0)--z0+z1+offset;

  folder = currentpicture;

  currentpicture := nullpicture;

  % This is used to line things up. It's the coords of the center of the
  % folder.
  pair foldercenter;
  pair folderbotcenter;
  pair foldertopcenter;
  %foldercenter = 0.5 (llcorner(folder) + urcorner(folder));
  foldercenter = center(folder);
  folderbotcenter = 0.5 (llcorner(folder) + lrcorner(folder));
  foldertopcenter = 0.5 (ulcorner(folder) + urcorner(folder));
  
  % Now things are set up so that I can say
  % draw folder shifted f;
  % label.top(btex blahblahblah etex,f + foldertopcenter);
  % label.bot(btex blahblahblah etex,f + folderbotcenter);
  % To get a label above or below the folder and properly centered,
  % where f is a pair indicating where the folder should be drawn.
  %
  % In fact, here are macros to do exactly that.
  % NOTE: It's a little weird and mysterious how these arguments get passed.
  % See the examples of how these are used. The btex/etex thing is weird
  % as is the fact that you don't need parenthesis, though you can use them
  % if you want.
  % The fact that you need two parenthesis around the z argument is odd too.
  % I think that's because it's declared text (which I must do since msg has
  % to be text).
  def labelfoldertop(text z, msg) =
    draw folder shifted z;
    label.top(msg,z + foldertopcenter);
  enddef;
  
  def labelfolderbot(text z,msg) =
    draw folder shifted z;
    label.bot(msg,z + folderbotcenter);
  enddef;
  
%  labelfoldertop((50,50))("blahblahblahandsoon");

%  labelfolderbot((0,100))(btex {\tt food} etex);
%  labelfolderbot((0,50))(btex {\tt grains} etex);
%  labelfolderbot((50,50))(btex {\tt fruit} etex);
%  draw (0,100)+folderbotcenter-(0,11) -- (0,50)+foldertopcenter+(0,4);
%  draw (0,100)+folderbotcenter-(0,11) --
%    0.5[(0,100)+folderbotcenter-(0,11),(0,50)+foldertopcenter+(0,4)] --
%    (xpart((50,50)+foldertopcenter+(0,4)),
%     ypart(0.5[(0,100)+folderbotcenter-(0,11),(0,50)+foldertopcenter+(0,4)])) --
%    (50,50)+foldertopcenter+(0,4);

%  draw (0,50)+folderbotcenter-(0,11) -- (xpart((0,50)+folderbotcenter),30);
%  label.urt(btex {\tt Oats} etex,(-2,20));
%  label.urt(btex {\tt Wheat} etex,(-2,11));
%  label.urt(btex {\tt Barley} etex,(-2,0));
%
%  draw (50,50)+folderbotcenter-(0,11) -- (xpart((50,50)+folderbotcenter),30);
%  label.urt(btex {\tt Apple} etex,(47,18));
%  label.urt(btex {\tt Orange} etex,(47,8));
%  
%      
%  labelfolderbot((100,100))(btex {\tt restaurant} etex);
%  label.urt(btex {\tt Menu} etex,(100,70));
%  draw (100,100)+folderbotcenter-(0,11) --
%    (xpart((100,100)+folderbotcenter-(0,11)), 80);
%
%  labelfolderbot((50,150))(btex {\tt classpath} etex);
  
%  draw (50,150)+folderbotcenter-(0,11) --
%    0.5[(50,150)+folderbotcenter-(0,11),
%        (xpart((50,150)+folderbotcenter-(0,11)),
%	 ypart((0,100)+foldertopcenter+(0,4)))] --
%    (xpart((0,100)+foldertopcenter+(0,4)),ypart(
%	0.5[(50,150)+folderbotcenter-(0,11),
%          (xpart((50,150)+folderbotcenter-(0,11)),
%	   ypart((0,100)+foldertopcenter+(0,4)))])) --
%    (0,100)+foldertopcenter+(0,4);
  
%  draw (50,150)+folderbotcenter-(0,11) -- 
%    0.5[(50,150)+folderbotcenter-(0,11),
%        (xpart((50,150)+folderbotcenter-(0,11)),
%	 ypart((0,100)+foldertopcenter+(0,4)))] --
%    (xpart((100,100)+foldertopcenter+(0,4)),ypart(
%	0.5[(50,150)+folderbotcenter-(0,11),
%          (xpart((50,150)+folderbotcenter-(0,11)),
%	   ypart((0,100)+foldertopcenter+(0,4)))])) --
%     (100,100)+foldertopcenter+(0,4);


  % Simple end-mill
  draw (0,30)--(0,0)--(5,0)--(5,30);
  label.bot(btex \tiny End-mill etex,(2,-5));

  % Ball-mill
  draw (40,30)--(40,5){down}..(42.5,0){right}..{up}(45,5)--(45,30);
  label.bot(btex \tiny Ball-mill etex,(42,-5));

  % Shanked mills.
  draw (81,30)--(81,15)--(78,15)--(78,0)--(87,0)--(87,15)--(84,15)--(84,30);
  draw (116,30)--(116,15)--(119,15)--(119,0)--(122,0)--(122,15)--(125,15)--(125,30);
  label.bot(btex \tiny Shanked End-mills etex,(100,-5));

  draw (158,30)--(158,0){right}..(162.5,5)..{right}(167,0)--(167,30);
  label.bot(btex \tiny Dimpled etex,(162,-5));

  % Next row of cutters
  draw (0,-40)--(0,-65)--(-10,-65)--(-10,-70)--(14,-70)--(14,-65)--(4,-65)--(4,-40);
  label.bot(btex \tiny Slit saw etex,(2,-75));

  draw (40,-40)--(40,-50){left}..(42,-70){right}..{left}(44,-50)--(44,-40);
  label.bot(btex \tiny Sphere mill etex,(42,-75));

  draw (80,-40)--(80,-55)--(73,-70)--(91,-70)--(84,-55)--(84,-40);
  label.bot(btex \tiny Dovetail etex,(82,-75));

  %draw (120,-40)--(120,-65)--(110,-65)--(110,-70)--(134,-70)--(134,-65)--(124,-65)--(124,-40);
  %draw (120,-40)--(120,-43)--(110,-65)--(110,-70)--(134,-70)--(134,-65)--(124,-65)--(124,-40);
  %                 six        five      four       three         two       one      center      one        two       three       four       five       six
  draw (120,-40)--(120,-43)--(118,-43)..(115,-50)..(120,-60)..(115,-65)--(116,-70)--(118,-70)--(128,-70)--(129,-65)..(124,-60)..(129,-50)..(126,-43)--(124,-43)--(124,-40);
  label.bot(btex \tiny Complex etex,(122,-75));

endfig;
end;