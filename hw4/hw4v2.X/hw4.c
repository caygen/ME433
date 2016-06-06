/*
 * File:   hw4.c
 * Author: Can
 *
 * Created on April 4, 2016, 2:49 PM
 */
// DEVCFG0
#pragma config DEBUG = OFF      // no debugging
#pragma config JTAGEN = OFF     // no jtag
#pragma config ICESEL = ICS_PGx1// use PGED1 and PGEC1
#pragma config PWP = OFF        // no write protect
#pragma config BWP = OFF        // no boot write protect
#pragma config CP = OFF         // no code protect

// DEVCFG1
#pragma config FNOSC = PRIPLL   // use primary oscillator with pll
#pragma config FSOSCEN = OFF    // turn off secondary oscillator
#pragma config IESO = OFF       // no switching clocks
#pragma config POSCMOD = HS     // high speed crystal mode
#pragma config OSCIOFNC = OFF   // free up secondary osc pins
#pragma config FPBDIV = DIV_1   // divide CPU freq by 1 for peripheral bus clock
#pragma config FCKSM = CSDCMD   // do not enable clock switch
#pragma config WDTPS = PS1048576 // slowest wdt
#pragma config WINDIS = OFF     // no wdt window
#pragma config FWDTEN = OFF     // wdt off by default
#pragma config FWDTWINSZ = WINSZ_25 // wdt window at 25%

// DEVCFG2 - get the CPU clock to 48MHz
#pragma config FPLLIDIV = DIV_2 // divide input clock to be in range 4-5MHz
#pragma config FPLLMUL = MUL_24 // multiply clock after FPLLIDIV
#pragma config FPLLODIV = DIV_2 // divide clock after FPLLMUL to get 48MHz
#pragma config UPLLIDIV = DIV_2 // divider for the 8MHz input clock, then multiply by 12 to get 48MHz for USB
#pragma config UPLLEN = ON      // USB clock on

// DEVCFG3
#pragma config USERID = 0xCCA0  // some 16bit userid, doesn't matter what
#pragma config PMDL1WAY = OFF   // allow multiple reconfigurations
#pragma config IOL1WAY = OFF    // allow multiple reconfigurations
#pragma config FUSBIDIO = ON    // USB pins controlled by USB module
#pragma config FVBUSONIO = ON   // USB BUSON controlled by USB module


#include<xc.h>                  // processor SFR definitions
#include<sys/attribs.h>         // __ISR macro
#include<math.h>
#include "i2c_master_noint.h"

#define CS LATBbits.LATB15
#define PI 3.14159265
#define i2c_addr 0b0100000
//#define CSother LATBbits.LATB#// chip select pin for future spi chip

//static volatile float sinWave[200],triWave[200];

void initSPI1(void) {
    SDI1Rbits.SDI1R = 0b0100;   // SDI1
    RPB13Rbits.RPB13R = 0b0011; // SDO1
    RPB15Rbits.RPB15R = 0b0011; // SS1
    ANSELBbits.ANSB15 = 0;
    TRISBbits.TRISB15 = 0;
    CS = 1;

    SPI1CON = 0;
    SPI1BUF;
    SPI1BRG = 0x7CF;          // SP1BRG = 7CF 1999 for debug (20kHz); SPI1BRG =(80000000/(2*desired))-1
    SPI1STATbits.SPIROV = 0;  // clear the overflow bit
    SPI1CONbits.CKE = 1;      // data changes when clock goes from hi to lo (since CKP is 0)
    SPI1CONbits.MSTEN = 1;    // master operation
    SPI1CONbits.ON = 1;       // turn on spi1
}

char SPI1_IO(unsigned char o) {
    SPI1BUF = o;
    while(!SPI1STATbits.SPIRBF) { // TX? RX?
        ;
    }
    return SPI1BUF;
}
void setVoltage(unsigned char channel, unsigned char voltage) {
    CS = 0;
    SPI1_IO((channel << 7 | 0b01110000)|(voltage >> 4));
    SPI1_IO(voltage << 4);
    int count = _CP0_GET_COUNT();
    while(_CP0_GET_COUNT() < count + 24000000/10000) {;}
    CS = 1;
}

void initExpander(void) {
    i2c_master_start();
    i2c_master_send((i2c_addr << 1) | 0); // bit 0 becomes 0 for write
    i2c_master_send(0x00); // IODIR register address
    i2c_master_send(0b10000000);
    i2c_master_stop();
}

void setExpander(char pin, char level) {
    i2c_master_start();
    i2c_master_send((i2c_addr << 1) | 0); // bit 0 becomes 0 for write
    i2c_master_send(0x0A);                  // OLAT register address
    i2c_master_send(level << pin);
    i2c_master_stop();
}

char getExpander(void) {
    i2c_master_start();
    i2c_master_send((i2c_addr << 1) | 0); // bit 0 becomes 0 for write
    i2c_master_send(0x09);                // GPIO register address
    i2c_master_restart();
    i2c_master_send((i2c_addr << 1) | 1); // bit 0 becomes 1 for read
    char r = i2c_master_recv();
    i2c_master_ack(1);                    // tell slave no more bytes requested
    i2c_master_stop();                    // stop bit
    return r;
}

int main() {

    __builtin_disable_interrupts();

    // set the CP0 CONFIG register to indicate that kseg0 is cacheable (0x3)
    __builtin_mtc0(_CP0_CONFIG, _CP0_CONFIG_SELECT, 0xa4210583);

    // 0 data RAM access wait states
    BMXCONbits.BMXWSDRM = 0x0;

    // enable multi vector interrupts
    INTCONbits.MVEC = 0x1;

    // disable JTAG to get pins back
    DDPCONbits.JTAGEN = 0;

    // do your TRIS and LAT commands here
    TRISAbits.TRISA4 = 0; //choose ra4 to be output pin (led)
    LATAbits.LATA4 = 0; 
    initSPI1();
    initExpander();
    i2c_master_setup();

    __builtin_enable_interrupts();

    int i=0; char voltage;
    int count = 0;
    while(1) {
//      if((getExpander() & 0b10000000) == 0b10000000) { // if pin 7 high
//          setExpander(0,1);                            // set pin 0 high
//      } else {
//          setExpander(0,0);                            // set pin 0 low
//      }
      _CP0_SET_COUNT(0);
      unsigned char Va = 0,Vb = 0;
      Va = 127 + 127*sin(2 * 3.14 * count * 4 );
      Vb = 255*count/1000;
      setVoltage(0,Va);
      setVoltage(1,Vb);
      while(_CP0_GET_COUNT() < 240) {;} //wait
      count++;
      if(count >= 1000) {               //reset counter
          count = 0;                    //
      }
      LATAINV = 0x010;                 //invert ra4
    }


}
