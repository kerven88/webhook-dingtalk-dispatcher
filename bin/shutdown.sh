#!/bin/bash

SCRIPT_DIR=`dirname $0`
PROJECT_DIR="$SCRIPT_DIR/.."
# 不要修改进程标记，作为进程属性关闭使用
PROCESS_FLAG="webhook-dingtalk-dispatcher-flag:${PROJECT_DIR}"
pkill -f $PROCESS_FLAG
echo 'Stop webhook-dingtalk-dispatcher!'