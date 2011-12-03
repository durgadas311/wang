#ifndef __wang_pport_h__
#define __wang_pport_h__

#ident "$Id: pport.h,v 1.2 2011/12/03 20:31:35 drmiller Exp $"

#include <sys/types.h>
#include <stdint.h>
#include <linux/ioctl.h>
#include <linux/parport.h>
#include <linux/ppdev.h>

extern int ppdev_setup(char *);
extern int ppdev_stat(void);
extern void ppdev_close(void);

extern int send_byte(uint8_t);
extern int recv_byte(uint8_t *);


#endif /* __wang_pport_h__ */
