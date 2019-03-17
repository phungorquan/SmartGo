#include "stm32f10x.h" // Device header
#include "math.h"
#include "delay.h"
#include "DirectionData.h"
#include "UART_Driver.h"
#include "SSD1306.h"

#define EARTH_RADIUS 6378.137
#define PI 3.14159265359
#define MESSAGE_DELIMITERS_STRING "()	, "

#define MPU9250_I2C I2C2
#define MPU9250_I2C_SCL_Pin GPIO_Pin_10
#define MPU9250_I2C_SDA_Pin GPIO_Pin_11
#define MPU9250_I2C_Speed 100000 // 100kHz standard mode
#define MPU9250_I2C_Port GPIOB

static DirectionDataListT list;

uint8_t messageToDirectionData(char *message, int len, uint8_t *nextDirection, uint32_t *longtitude, uint32_t *latetidude);
void UART2_ReceiveMessage(char *message, uint8_t length);
float coordinatesToMeters(float lat1, float long1, float lat2, float long2);
void myI2C_Init(void);

//ngat()
//{
//	DirectionDataT *ptr = DirectionDataList_Get(&list);
//	ptr->
//	free(ptr);
//}

int main(void)
{
	
	delay_Init();
	UART_Init(UART2_ReceiveMessage);
	myI2C_Init();

	DirectionDataList_Init(&list);
	UART_SendStr("SmartGo\n\0");
	

	delay_us(50000); // 50ms

	init(0x3C);

	display();

	while (1)
	{
	}
}

uint8_t messageToDirectionData(char *message, int len, uint8_t *nextDirection, uint32_t *longtitude, uint32_t *latetidude)
{
	char *tokenPointer;

	if (message != NULL && message[0] == '(' && message[len - 2] == ')')
	{
		tokenPointer = strtok(message, MESSAGE_DELIMITERS_STRING);
		*nextDirection = atoi(tokenPointer);
		tokenPointer = strtok(NULL, MESSAGE_DELIMITERS_STRING);
		if (tokenPointer != NULL)
		{
			*longtitude = atoi(tokenPointer);
		}
		else
		{
			return 0;
		}

		tokenPointer = strtok(NULL, MESSAGE_DELIMITERS_STRING);
		if (tokenPointer != NULL)
		{
			*latetidude = atoi(tokenPointer);
		}
		else
		{
			return 0;
		}

		tokenPointer = strtok(NULL, MESSAGE_DELIMITERS_STRING);
		if (tokenPointer == NULL)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	else
	{
		*nextDirection = 0;
		*longtitude = 0;
		*latetidude = 0;

		return 0;
	}
}

void UART2_ReceiveMessage(char *message, uint8_t length)
{
	uint8_t nextDi;
	uint32_t longtitude, latetitude;
	int result;

	UART_SendStr(message);

	if (messageToDirectionData(message, length, &nextDi, &longtitude, &latetitude) == 1)
	{
  		result = DirectionDataList_Put(&list, nextDi, longtitude, latetitude);
//		if (result == STATUS_OK)
//		{
//			GPIO_ResetBits(GPIOB, GPIO_Pin_4);
//			GPIO_ResetBits(GPIOB, GPIO_Pin_14);
//			GPIO_ResetBits(GPIOA, GPIO_Pin_0);
//			GPIO_ResetBits(GPIOA, GPIO_Pin_7);
//	
//		}
//		else if (result == STATUS_EXCEED_LIMIT)
//		{
//			GPIO_SetBits(GPIOB, GPIO_Pin_4);
//			GPIO_SetBits(GPIOA, GPIO_Pin_7);
//		
//		}
//		else if (result == STATUS_NOT_ENOUGH_MEMORY)
//		{
//			GPIO_SetBits(GPIOB, GPIO_Pin_4);
//			GPIO_SetBits(GPIOB, GPIO_Pin_14);
//			GPIO_SetBits(GPIOA, GPIO_Pin_0);
//			GPIO_SetBits(GPIOA, GPIO_Pin_7);
//		}
//		else
//		{
//			GPIO_ResetBits(GPIOB, GPIO_Pin_4);
//			GPIO_ResetBits(GPIOB, GPIO_Pin_14);
//			GPIO_ResetBits(GPIOA, GPIO_Pin_0);
//			GPIO_ResetBits(GPIOA, GPIO_Pin_7);
//		}
	}
}

float coordinatesToMeters(float lat1, float long1, float lat2, float long2)
{
	float dLat = lat2 * PI / 180 - lat1 * PI / 180;
	float dLon = long2 * PI / 180 - long1 * PI / 180;
	float a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1 * PI / 180) * cos(lat2 * PI / 180) * sin(dLon / 2) * sin(dLon / 2);
	float c = 2 * atan2(sqrt(a), sqrt(1 - a));
	float d = EARTH_RADIUS * c;
	return d * 1000;
}

void myI2C_Init(void)
{
	I2C_InitTypeDef I2C_InitStructure;
	GPIO_InitTypeDef GPIO_InitStructure;

	/* Configure I2C pins: SCL and SDA */
	GPIO_InitStructure.GPIO_Pin = MPU9250_I2C_SCL_Pin | MPU9250_I2C_SDA_Pin;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_OD;
	GPIO_Init(MPU9250_I2C_Port, &GPIO_InitStructure);

	/* I2C configuration */
	I2C_InitStructure.I2C_Mode = I2C_Mode_I2C;
	I2C_InitStructure.I2C_DutyCycle = I2C_DutyCycle_2;
	I2C_InitStructure.I2C_OwnAddress1 = 0x00; // MPU6050 7-bit adress = 0x68, 8-bit adress = 0xD0;
	I2C_InitStructure.I2C_Ack = I2C_Ack_Enable;
	I2C_InitStructure.I2C_AcknowledgedAddress = I2C_AcknowledgedAddress_7bit;
	I2C_InitStructure.I2C_ClockSpeed = MPU9250_I2C_Speed;

	/* Apply I2C configuration after enabling it */
	I2C_Init(MPU9250_I2C, &I2C_InitStructure);
	/* I2C Peripheral Enable */
	I2C_Cmd(MPU9250_I2C, ENABLE);
	
	
}
