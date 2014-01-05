/*
 * Internals of warping Wang 600 programs through GCC
 *
 *
 */

#ifndef __wpcc_wang600internals_h__
#define __wpcc_wang600internals_h__

.ident "Wang 600 Compiler over GCC $Revision: 1.15 $ "

.section .wang600code, "a";
	.include "wang600opcodes.s";
.pushsection .wang600regs,"a";
	.subsection 0;
		.type longreg_base STT_OBJECT;
		longreg_base:;
	.subsection 1;
		.byte 0;
.popsection

#define _shadow_code(n)	 \
			.pushsection .wang600dummy,"a"; \
				.rept n; .byte 0; .endr; \
			.popsection

#define _bytecode(__byte)	.byte (__byte); _shadow_code(1)

#define _opcode(op)		.byte (_op_ ##op ); _shadow_code(1)
#define _opreg(op,reg)		.byte (_op_ ##op ##reg ); _shadow_code(1)

#define _oplabel(prefix,label)	.byte ( ##prefix ##label ),( ##label ); \
					_shadow_code(2)

#define _longreg(reg)		.pushsection .wang600regs,1,"a"; \
					.type _longreg_ ##reg STT_OBJECT;	\
					.global _longreg_ ##reg ;	\
					.set _longreg_ ##reg ,longreg_base+(longreg_base-.); \
					.byte 0;	\
				.popsection

#define _longregs(reg,num)	.pushsection .wang600regs,1,"a"; \
					.type _longreg_ ##reg STT_OBJECT;	\
					.global _longreg_ ##reg ;	\
					.rept (num-1);			\
					.byte 0;			\
					.endr;				\
					.set _longreg_ ##reg ,longreg_base+(longreg_base-.); \
					.byte 0;	\
				.popsection

#define _regdata(reg,b0,b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,b11,b12,b13,b14,b15)	\
				.pushsection .wang600regs,"a"; \
					.subsection 0; \
					.type _longreg_ ##reg STT_OBJECT; \
					.global _longreg_ ##reg ; \
					.set _longreg_ ##reg ,longreg_base+(longreg_base-.); \
					.byte 0;	\
				.section .wang600data,"a"; \
					.align 8;		\
					.byte ((b15) << 4) | (b14);	\
					.byte ((b13) << 4) | (b12);	\
					.byte ((b11) << 4) | (b10);	\
					.byte ((b9) << 4) | (b8);	\
					.byte ((b7) << 4) | (b6);	\
					.byte ((b5) << 4) | (b4);	\
					.byte ((b3) << 4) | (b2);	\
					.byte ((b1) << 4) | (b0);	\
				.popsection

#define _oplongreg(op,reg)	_opcode(op); _bytecode(_longreg_ ##reg)

/***************************************************************************/

#define BEGIN()
#define END()

#define RES_EXTERN(label, const)	\
				.pushsection .wang600label,"a"; \
					.type label STT_OBJECT; \
					.global label ; \
					.set label , res_label ##const ; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					.global _search_ ##label ; \
					.set _search_ ##label , 0x80; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					.global _call_ ##label ; \
					.set _call_ ##label , 0xf7; \
				.popsection

#define RES_FEXTERN(flabel, const)	\
				.pushsection .wang600flabel,"a"; \
					.type flabel STT_OBJECT; \
					.global flabel ; \
					.set flabel , res_flabel ##const ; \
				.section .wang600search,"a"; \
					.type _search_ ##flabel STT_OBJECT; \
					.global _search_ ##flabel ; \
					.set _search_ ##flabel , 0x80; \
				.section .wang600call,"a"; \
					.type _call_ ##flabel STT_OBJECT; \
					.global _call_ ##flabel ; \
					.set _call_ ##flabel , 0xf7; \
				.section .wang600subr,"a"; \
					.type _subr_ ##flabel STT_OBJECT; \
					.global _subr_ ##flabel ; \
					.set _subr_ ##flabel , res_flabel ##const; \
				.popsection

// Define a label for use with SEARCH/MARK
#define LABEL(label)		.pushsection .wang600label,"a"; \
					.type label STT_OBJECT; \
					.global label; \
					label : .byte 0; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					.global _search_ ##label ; \
					_search_ ##label :; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					.global _call_ ##label ; \
					_call_ ##label :; \
				.popsection

// Define a label for use with SEARCH/MARK
#define LLABEL(label)		.pushsection .wang600label,"a"; \
					.type label STT_OBJECT; \
					label : .byte 0; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					_search_ ##label :; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					_call_ ##label :; \
				.popsection

// Define a label for use with FCALL/MARK
#define FLABEL(label)		.pushsection .wang600flabel,"a"; \
					.type label STT_OBJECT; \
					.global label ; \
					label :  .byte 0; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					.global _search_ ##label ; \
					_search_ ##label :; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					.global _call_ ##label ; \
					_call_ ##label :; \
				.section .wang600subr,"a"; \
					.type _subr_ ##label STT_OBJECT; \
					.global _subr_ ##label ; \
					_subr_ ##label :  .byte 0; \
				.popsection

// Define a label for use with FCALL/MARK
#define FLLABEL(label)		.pushsection .wang600flabel,"a"; \
					.type label STT_OBJECT; \
					label :  .byte 0; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					_search_ ##label :; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					_call_ ##label :; \
				.section .wang600subr,"a"; \
					.type _subr_ ##label STT_OBJECT; \
					_subr_ ##label :  .byte 0; \
				.popsection

// A label from another module for use with SEARCH/CALL
#define EXTERNAL(label)		.pushsection .wang600label,"a"; \
					.type label STT_OBJECT; \
					.global label ; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					.global _search_ ##label ; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					.global _call_ ##label ; \
				.popsection

// A label from another module for use with FCALL
#define FEXTERNAL(label)	.pushsection .wang600flabel,"a"; \
					.type label STT_OBJECT; \
					.global label ; \
				.section .wang600search,"a"; \
					.type _search_ ##label STT_OBJECT; \
					.global _search_ ##label ; \
				.section .wang600call,"a"; \
					.type _call_ ##label STT_OBJECT; \
					.global _call_ ##label ; \
				.section .wang600subr,"a"; \
					.type _subr_ ##label STT_OBJECT; \
					.global _subr_ ##label ; \
				.popsection

#define RES_REG(label,reg)	\
					.type _longreg_ ##label STT_OBJECT; \
					.global _longreg_ ##label; \
					.set _longreg_ ##label , reg

// Reserve an un-initialize long register
#define UREG(name)		_longreg(name)

// Reserve an array of "num" longregs
#define UREGS(name,num)		_longregs(name,num)

/* These should be pre-rpocessed and never exist when gcc invoked */
#define ENTER(num)		.error "run w6cpp preprocessor for ENTER()"
#define IREG_DATA(reg,num)	.error "run w6cpp preprocessor for IREG_DATA()"
#define ALPHA_STRING(str)	.error "run w6cpp preprocessor for ALPHA_STRING()"
#define ALPHA_PLOT(str)		.error "run w6cpp preprocessor for ALPHA_PLOT()"
#define ALPHA_TTY(str)		.error "run w6cpp preprocessor for ALPHA_TTY()"

// This forces us to post-process Wang600 programs now...
#define ENTER_REGNO(name)	_bytecode(0xf8); \
				_bytecode(0xf8); \
				_bytecode(_longreg_ ##name )

// since we have to post-process, might as well simplify this too
#define ENTER_LAST_REGNO()	_bytecode(0xf8); \
				_bytecode(0xf8); \
				_bytecode(__last_reg)

// Pseudo constructs for embedding data (future plan)
#define NAME(prog_name)		.pushsection .wang600name,"a",@note; \
					.string " prog_name "; \
					.popsection

#define AUTHOR(name)		.pushsection .wang600author,"a",@note; \
					.string " name "; \
					.popsection

#define TITLE(title)		.pushsection .wang600title,"a",@note; \
					.string " title "; \
					.popsection

#define HELP_BEGIN(string)	.pushsection .wang600help,"a",@note; \
					.string " string ";"\
					.popsection

#endif /* __wpcc_wang600internals_h__ */
