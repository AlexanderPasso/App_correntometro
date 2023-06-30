#include <SoftwareSerial.h>



//Definicion de variables y constantes

volatile int CantPulsos = 0;          //Variable que acumula los pulsos recibidos 
float LxM;                        //Variable que almacena el calculo de Litros x minuto
uint8_t  pinSensor = 2;            //Pin del arduino donde se conecta el sensor
float FacConv = 0.2;              //Factor de conversión para calcular velocidad y caudal (calibración)
float Area = 0.001520530844;      //Area transversal de la cavidad del sensor
float Velocidad = 0.0;
float Caudal=0.0;



SoftwareSerial BT1(10,11);        //Rx || Tx  pin 10 --> Tx del bluetooth   pin 11 ---> Rx del bluetooth

void rpm(){
  CantPulsos++ ;
}

//---Función para obtener frecuencia de los pulsos--------
int ObtenerFrecuencia() 
{
  int frecuencia;
  CantPulsos = 0;   //Ponemos a 0 el número de pulsos
  interrupts();    //Habilitamos las interrupciones
  delay(1000);   //muestra de 1 segundo
  noInterrupts(); //Desabilitamos las interrupciones
  frecuencia=CantPulsos; //Hz(pulsos por segundo)
  return frecuencia;
}


void setup() {
  Serial.begin(9600);
  // put your setup code here, to run once:
  pinMode(pinSensor, INPUT_PULLUP);
  
    
  attachInterrupt(digitalPinToInterrupt(pinSensor), rpm, RISING);         //Interrupcion pin 2 activa la funcion rpm
  BT1.begin(9600);

 
}

void loop() {
  // put your main code here, to run repeatedly:


  int frecuenciaPulsos = ObtenerFrecuencia(); //obtenemos la Frecuencia de los pulsos en Hz

  // Realizo los cálculos
  LxM = (frecuenciaPulsos / FacConv);                         //Calcula los Litros por Minuto 
  Velocidad = (LxM / Area)*(0.000016667);                     //Calculo la velocidad 
  Caudal = (LxM/60000);                                       //Caudal en m³/seg
  Serial.print(frecuenciaPulsos); Serial.print(","); Serial.print(LxM , 3); Serial.print(","); Serial.print(Velocidad , 3); Serial.print(","); Serial.println(Caudal , 8);  //Muetsro los datos por el serial
  BT1.print(frecuenciaPulsos); BT1.print(","); BT1.print(Velocidad,4); BT1.print(","); BT1.println(Caudal, 8);     //Envio los datos por bluetooth

}
