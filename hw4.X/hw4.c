/* 
 * File:   hw1.c
 * Author: Can
 *
 * Created on April 4, 2016, 2:49 PM
 */
// DEVCFG0
#pragma config DEBUG = OFF // no debugging
#pragma config JTAGEN = OFF // no jtag
#pragma config ICESEL = ICS_PGx1 // use PGED1 and PGEC1
#pragma config PWP = OFF // no write protect
#pragma config BWP = OFF // no boot write protect
#pragma config CP = OFF // no code protect

// DEVCFG1
#pragma config FNOSC = PRIPLL // use primary oscillator with pll
#pragma config FSOSCEN = OFF // turn off secondary oscillator
#pragma config IESO = OFF // no switching clocks
#pragma config POSCMOD = HS // high speed crystal mode
#pragma config OSCIOFNC = OFF // free up secondary osc pins
#pragma config FPBDIV = DIV_1 // divide CPU freq by 1 for peripheral bus clock
#pragma config FCKSM = CSDCMD // do not enable clock switch
#pragma config WDTPS = PS1048576 // slowest wdt
#pragma config WINDIS = OFF // no wdt window
#pragma config FWDTEN = OFF // wdt off by default
#pragma config FWDTWINSZ = WINSZ_25 // wdt window at 25%

// DEVCFG2 - get the CPU clock to 48MHz
#pragma config FPLLIDIV = DIV_2 // divide input clock to be in range 4-5MHz
#pragma config FPLLMUL = MUL_24 // multiply clock after FPLLIDIV
#pragma config FPLLODIV = DIV_2 // divide clock after FPLLMUL to get 48MHz
#pragma config UPLLIDIV = DIV_2 // divider for the 8MHz input clock, then multiply by 12 to get 48MHz for USB
#pragma config UPLLEN = ON // USB clock on

// DEVCFG3
#pragma config USERID = 0xCCA0  // some 16bit userid, doesn't matter what
#pragma config PMDL1WAY = OFF // allow multiple reconfigurations
#pragma config IOL1WAY = OFF // allow multiple reconfigurations
#pragma config FUSBIDIO = ON // USB pins controlled by USB module
#pragma config FVBUSONIO = ON // USB BUSON controlled by USB module


#include<xc.h>           // processor SFR definitions
#include<sys/attribs.h>  // __ISR macro
#include<math.h>
#define CSdac LATBbits.LATB15// chip select pin
//#define CSother LATBbits.LATB#// chip select pin for future spi chip

static volatile float sinWave[200],triWave[200];

void initSPI1(){
    RPB13Rbits.RPB13R = 0b0011;     //SDO1  = b13
    //RPB15Rbits.RPB15R = 0b0011;     //SS1   = b15
    TRISBbits.TRISB15 = 0;          //CSdac = b15 = SS1
    CSdac = 1;
    //CSother = 1;
    //setup spi1
    SPI1CON = 0;              // turn off the spi module and reset it
    SPI1BUF;                  // clear the rx buffer by reading from it
    SPI1BRG = 0x2;            // baud rate to 10 MHz [SPI4BRG = (48000000/(2*desired))-1] = 1.4 so pick the next int = 2
    SPI1STATbits.SPIROV = 0;  // clear the overflow bit
    SPI1CONbits.CKE = 1;      // data changes when clock goes from hi to lo (since CKP is 0)
    SPI1CONbits.MSTEN = 1;    // master operation
    SPI1CONbits.ON = 1;       // turn on spi 1
}

char SPI1_IO(unsigned char o){
          SPI1BUF = o;
      while(!SPI1STATbits.SPIRBF) { // wait to receive the byte
    ;
  }
  return SPI1BUF;
}

void setVoltage(char channel, char voltage){
    CSdac = 0;
    unsigned char message = 0x70;
    if (channel == 'B'){
        message |= 0x80;
    }
//    else if(channel != 'A'){  message |= 0x70;  }
    if (voltage >= 3.3){voltage = 3.3;}
    if (voltage <= 0){voltage = 0;}
    char temp = voltage / 3.3 * 255.0;
    temp << 4;
    message |= temp;
    SPI1_IO(message);
    CSdac = 1;
}

//this is modified verion of a ME333 hw from last quarter
void MakeWave(){  
    int i;
    for(i=0; i<200; i++){
        sinWave[i]=1.65*(1+sin(2*M_PI*10*i/1000));
        triWave[i]=i*3.3/200;
    }
}

void initI2C(){
    ANSELBbits.ANSB2 = 0;
    ANSELBbits.ANSB3 = 0;
    I2C2BRG = 233;              // =[1/(2*Fsck)-PGD]*Pblck - 2
    I2C2CONbits.ON = 1;        // turn on the I2C1 module
}

void initExpander(){
    
}
void setExpander(char pin, char level);
char getExpander();


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
    TRISBbits.TRISB8 = 0; //choose rb8 to be output pin
    TRISBbits.TRISB7 = 1; //choose rb7 to be input pin 
    __builtin_enable_interrupts();
    
    MakeWave();
    initSPI1();
    int i; char voltage;
    while(1) {
	    // use _CP0_SET_COUNT(0) and _CP0_GET_COUNT() to test the PIC timing
		// remember the core timer runs at half the CPU speed
        _CP0_SET_COUNT(0);
        while(_CP0_GET_COUNT()<24000){;} //wait 1/1000sec
        LATBINV = 0x100;                 //invert rb8
        _CP0_SET_COUNT(0);               //reset core timer
        while(PORTBbits.RB7 == 1){        // wait if button pressed
        LATBCLR = 0x100;        // clear the led while the button is pressed
        }
        setVoltage('A',sinWave[i]);
        setVoltage('B',triWave[i]);
        i++;
        if(i>=199){i=0;}
    }
    
    
}