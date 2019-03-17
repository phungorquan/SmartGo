#include "UART_Driver.h"

/*
USART2
PA2-TX
PA3-RX
*/

void UART_Init(void (*_UART_Received_CallBack)(char *, uint8_t))
{
	/* Init Structure */
	USART_InitTypeDef USART_InitStructure;
	GPIO_InitTypeDef GPIO_InitStructure;
	NVIC_InitTypeDef NVIC_InitStructure;
	
	/* GPIO Init */
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_GPIOA, ENABLE);
	RCC_APB2PeriphClockCmd(RCC_APB2Periph_AFIO, ENABLE);
	
	/* TX - PA2 */
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_2;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_AF_PP;
	GPIO_Init(GPIOA, &GPIO_InitStructure);
	
	/* RX - PA3 */
	GPIO_InitStructure.GPIO_Pin = GPIO_Pin_3;
	GPIO_InitStructure.GPIO_Speed = GPIO_Speed_50MHz;
	GPIO_InitStructure.GPIO_Mode = GPIO_Mode_IN_FLOATING;
	GPIO_Init(GPIOA, &GPIO_InitStructure);
	
	/* NVIC Config */
	NVIC_InitStructure.NVIC_IRQChannel = USART2_IRQn;
  NVIC_InitStructure.NVIC_IRQChannelSubPriority = 0;
  NVIC_InitStructure.NVIC_IRQChannelCmd = ENABLE;
  NVIC_Init(&NVIC_InitStructure);
	
	/* Enable Clock for USART2 */
	RCC_APB1PeriphClockCmd(RCC_APB1Periph_USART2, ENABLE);
	
	/* Configuration */
	USART_DeInit(USART2);
	USART_InitStructure.USART_BaudRate = 9600;
  USART_InitStructure.USART_WordLength = USART_WordLength_8b;
  USART_InitStructure.USART_StopBits = USART_StopBits_1;
  USART_InitStructure.USART_Parity = USART_Parity_No;
  USART_InitStructure.USART_HardwareFlowControl = USART_HardwareFlowControl_None;
  USART_InitStructure.USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
	USART_Init(USART2, &USART_InitStructure);
	
	/* Interrupt */
	USART_ITConfig(USART2, USART_IT_RXNE, ENABLE);    // Xay ra ngat khi thanh ghi du lieu nhan cua USART2 day 
	
	/* Enable USART2 */
	USART_Cmd(USART2, ENABLE);
	
	/* Allocate buffer */
	_buffer = (char*) malloc(RECEIVE_BUFFER_SIZE * sizeof(char));
	_sendBuffer = (char*) malloc(RECEIVE_BUFFER_SIZE * sizeof(char));
	
	/* Set callback function */
	UART_Received_CallBack = _UART_Received_CallBack;
}

void UART_Send(char ch)
{
	while(USART_GetFlagStatus(USART2, USART_FLAG_TC) == RESET);
	USART_SendData(USART2, (uint8_t) ch);
}

void UART_SendStr(char* str)
{
	while (*str != '\0') 
	{
		UART_Send(*str);
		str++;
	}
}

void USART2_IRQHandler(void)
{
	/* RXNE handler */
	if(USART_GetITStatus(USART2, USART_IT_RXNE) != RESET)
	{
		if (_index <= RECEIVE_BUFFER_SIZE) _buffer[_index++] = (char)USART_ReceiveData(USART2); /* If _buffer is available, add received char to _buffer */
		if (_buffer[_index - 1] == '\0') /* If the received char is \0, send it to user's function */
		{
			strcpy(_sendBuffer, _buffer);
			UART_Received_CallBack(_sendBuffer, _index);
			_index = 0;
		}
	
		/* Clear flag */
		USART_ClearITPendingBit(USART2, USART_IT_RXNE);
	}
}
