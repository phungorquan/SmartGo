#include "DirectionData.h"

void DirectionDataList_Init(DirectionDataListT *list)
{
    list->pHead = NULL;
    list->pTail = NULL;
    list->count = 0;
}

AddResultCode DirectionDataList_Put(DirectionDataListT *list, int nextDirection, float longtitude, float latetitude)
{
    if (list->count < DIRECTION_DATA_LIST_NODE_LIMIT)
    {
        DirectionDataT *data = (DirectionDataT *)malloc(sizeof(DirectionDataT));
        if (data == NULL)
        {
            return STATUS_NOT_ENOUGH_MEMORY;
        }
        data->nextDirection = nextDirection;
        data->longtitude = longtitude;
        data->latetitude = latetitude;

        DirectionDataNodeT *node = (DirectionDataNodeT *)malloc(sizeof(DirectionDataNodeT));
        if (node == NULL)
        {
            return STATUS_NOT_ENOUGH_MEMORY;
        }
        node->data = data;
        node->pNext = NULL;

        if (list->count == 0)
        {
            list->pHead = node;
            list->pTail = node;
        }
        else
        {
            list->pTail->pNext = node;
            list->pTail = node;
        }
        list->count++;

        return STATUS_OK;
    }
    else
    {
        return STATUS_EXCEED_LIMIT;
    }
}

DirectionDataT *DirectionDataList_Get(DirectionDataListT *list)
{
    if (list->count > 0)
    {
        DirectionDataNodeT *node = list->pHead;
        DirectionDataT *data = node->data;

        if (--(list->count) == 0)
        {
            list->pHead = NULL;
            list->pTail = NULL;
        }
        else
        {
            list->pHead = node->pNext;
        }

        free(node);
        // *data will be free by user.

        return data;
    }
    else
    {
        return NULL;
    }
}

void DirectionDataList_Clear(DirectionDataListT *list)
{
    DirectionDataNodeT *node;

    while (list->pHead != NULL)
    {
        node = list->pHead;
        list->pHead = node->pNext;

        free(node->data);
        free(node);
    }

    DirectionDataList_Init(list);
}
