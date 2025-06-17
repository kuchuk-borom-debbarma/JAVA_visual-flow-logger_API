# Visual Flow Logger
VFL is a visual logger that makes can be used to create systematic structured log, 
making it easier to visualize the flow of the logs as the program goes on. <br>

It aims to provide both local and distributed logging but will be starting off with local logging first.

# VFL Design Philosophy
## VFL Block
VFL Block is a "container" or "section" whose purpose is to hold linked list of logs in order they appeared. <br>
A block will have sequential logs which can then dive into nested blocks signifying the flow of the operation.
## VFL Log
VFL Log is the actual logging data stored in a block. They can be as simple as a message or can be starting of another sub-block and much more.

# Implementation Design 
## Goal
On it's heart the ```VisFlowLogger``` class is what is going to act as a block. <br>
It is meant to be used as the logger instance for logging that certain block. <br>
When it's time to define a sub-block, it must be done by calling a method in the ```VisFlowLogger``` to create a sub-block.<br>
## Implementation
### VisFlowLogger
This class acts as the block logger instance and this is what must be used to create sequential log for 