[when]after {value}								= RuleResult( name == "{value}");
[when]before {value}							= not RuleResult( name == "{value}");
[when]filename matches "{value}"	= RuleInput( fileName matches "{value}" );
[when]header matches "{value}"		= RuleInput( header matches "{value}" );
[when]mime type is {value}				= RuleInput( mimeType == "{value}" );
[then]use {value} = insert(new RuleResult("{value}")); output.add("{value}");
