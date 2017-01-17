---
layout: docs
title: Architecture
permalink: /docs/architecture/
---

XOR is a layer that sits between the application code and the ORM.
The main motivation in creating XOR was the difficulty in solving performance issues with an ORM.
<div class="note info">
  <h5>Incrementally add XOR functionality into your code</h5>
  <p>XOR can be very useful to target a poorly performing piece of ORM code. The more complex your data model is, the more benefits you will achieve using XOR.</p>
</div>

Since the ORM hides a lot of the generated SQL from the developer, it becomes very difficult to fine tune a badly performing query. With XOR, this information can be explicit defined, allowing a DBA to fine tune it further if necessary.

The diagram below shows where ORM fits in the code stack.

![](/img/XORarch.png)
