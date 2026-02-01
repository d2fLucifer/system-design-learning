# System Design – Rate Limiter

## Overview

A **Rate Limiter** controls the number of requests a client can send within a given time window in order to:
- Protect backend services from overload
- Prevent abuse (spam, brute-force, DDoS)
- Ensure fair resource usage across clients

---

## Requirements

### Functional Requirements
- **Accurately limit excessive requests**  
  Prevent clients from exceeding the allowed request rate.
- **Clear exception handling**  
  Return explicit errors such as HTTP `429 Too Many Requests`.

### Non-Functional Requirements
- **Low latency**  
  Rate limiting must not significantly impact request latency.
- **Memory efficiency**  
  Minimize memory usage at scale.
- **Distributed rate limiting**  
  Support multiple servers sharing rate limit state.
- **High fault tolerance**  
  Failure of the rate limiter (e.g., cache outage) must not crash the system.

---

## Algorithms for Rate Limiting

Common algorithms and their trade-offs:

- Token Bucket
- Leaky Bucket
- Fixed Window Counter
- Sliding Window Log
- Sliding Window Counter

---

## Token Bucket Algorithm

### Concept

Requests are allowed as long as tokens are available.  
Tokens are added at a fixed rate, up to a maximum capacity.

This allows **short bursts** while enforcing a **long-term average rate**.

---

### How It Works

- A bucket has a **fixed capacity**
- Tokens are added at a **constant refill rate**
- When the bucket is full, no additional tokens are added

![[file-20260201104422354.png]]

- Each incoming request consumes **one token**
    - If tokens are available → request is allowed
    - If no tokens are available → request is rejected

---

### Parameters

- **Bucket size (capacity)**  
  Maximum number of tokens the bucket can hold  
  → Controls burst tolerance

- **Refill rate**  
  Tokens added per second  
  → Controls long-term throughput

---

### Characteristics

- Allows controlled burst traffic
- O(1) time complexity
- Easy to implement with Redis
- Widely used in API gateways

---

## Leaky Bucket Algorithm

### Concept

Requests are processed at a **fixed output rate**, typically using a FIFO queue.

---

### How It Works

- Each client has a queue (bucket)
- When a request arrives:
    - If the queue is not full → enqueue
    - If the queue is full → drop request
- Requests are dequeued and processed at a fixed rate

![[Self-learning knowledge/Books/System Design/System Design Alex Xu/assets/Design a rate limiter/14851366e9c3533783f70f1df902b3ca_MD5.jpg]]

---

### Parameters

- **Bucket size**  
  Maximum number of queued requests
- **Outflow rate**  
  Number of requests processed per second

---

### Pros
- Memory usage is bounded
- Stable and predictable output rate

### Cons
- Burst traffic fills the queue quickly
- Old requests may block newer ones
- Hard to tune parameters correctly

---

## Fixed Window Counter Algorithm

### Concept

The timeline is divided into **fixed-size windows**.  
Each window maintains a request counter.

---

### How It Works

- Each request increments the counter
- When the counter reaches the limit, further requests are rejected
- Counter resets when a new window starts

![[Self-learning knowledge/Books/System Design/System Design Alex Xu/assets/Design a rate limiter/eacb999dea8847a5fb1ca03a2ef5aaf4_MD5.jpg]]

**Example:**  
Allow at most **3 requests per second**

---

### Pros
- Very simple
- Extremely memory efficient

### Cons
- Boundary problem (traffic spikes at window edges)
- Inaccurate under burst traffic

---

## Sliding Window Log Algorithm

### Concept

Maintain a log of request timestamps and enforce limits over a rolling window.

---

### How It Works

- Store request timestamps (e.g., Redis sorted set)
- On each request:
    1. Remove timestamps older than `now - window_size`
    2. If remaining count ≤ limit → allow
    3. Else → reject

![[Self-learning knowledge/Books/System Design/System Design Alex Xu/assets/Design a rate limiter/ef07e8d5e818a8ac3b89fe58b3928dc8_MD5.jpg]]

---

### Example (2 requests per minute)

- 1:00:01 → allowed (log size = 1)
- 1:00:30 → allowed (log size = 2)
- 1:00:50 → rejected (log size = 3)
- 1:01:40 → outdated timestamps removed → allowed

---

### Pros
- Very accurate
- No boundary issues

### Cons
- High memory usage
- Poor scalability under high traffic

---

## Sliding Window Counter Algorithm

### Concept

Hybrid approach combining Fixed Window Counter and Sliding Window Log.

---

### How It Works

Approximate the rolling window using weighted counters:

- effective_requests =  current_window_count +  previous_window_count × overlap_ration
- [[Self-learning knowledge/Books/System Design/System Design Alex Xu/assets/Design a rate limiter/b9eed183264a960d4c00fd635456c4a2_MD5.jpg|Open: file-20260201142058140.png]]
  ![[Self-learning knowledge/Books/System Design/System Design Alex Xu/assets/Design a rate limiter/b9eed183264a960d4c00fd635456c4a2_MD5.jpg]]
---

### Pros
- More accurate than fixed window
- Less memory than sliding window log

### Cons
- Approximation, not exact
- Slightly more complex logic

---

## Summary & Interview Recommendation

- **Best default choice**: Token Bucket
- **Traffic shaping**: Leaky Bucket
- **Simple, low scale**: Fixed Window
- **High accuracy required**: Sliding Window Log

---

## Key Takeaways

- Token Bucket is the industry standard for APIs
- Burst handling is critical for real-world systems
- Trade-offs are always between accuracy, memory, and complexity
