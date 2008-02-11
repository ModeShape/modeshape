[when]after {value}								= SequencerInfo( name == "{value}");
[when]before {value}							= not SequencerInfo( name == "{value}");
[when]file name matches "{value}"	= ContentInfo( fileName matches "{value}" );
[when]extension matches "{value}"	= ContentInfo( fileName matches "*./.{value}$" );
[when]header matches "{value}"		= ContentInfo( header matches "{value}" );
[when]mime type is {value}				= ContentInfo( mimeType == "{value}" );
[then]use {value} = insert(new SequencerInfo("{value}")); sequencers.add("{value}");
