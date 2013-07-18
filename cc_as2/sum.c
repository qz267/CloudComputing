#include <stdio.h>
 
int main(void)
{
    int a, b, result; //宣告需要用到的變數
 
    printf("這個程式計算兩個整數的和....\n");
    printf("請輸入第一個整數： "); //提示使用者輸入的文字
    scanf("%d", &a);
    printf("請輸入第二個整數： "); //提示使用者輸入的文字
    scanf("%d", &b);
    result = a + b; //計算結果
     
    printf("結果如下\n");
    printf("%d + %d = %d\n", a, b, result);    
     
    return 0;
}