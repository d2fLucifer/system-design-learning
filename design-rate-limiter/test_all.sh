#!/bin/bash

# Configuration
BASE_URL="http://127.0.0.1:8080/api/v1"
CAPACITY=5
REFILL_RATE=1
TEST_KEY="automated_test_user"
LOAD_TEST_COUNT=1000000 # 1 Million requests
LOAD_CONCURRENCY=200

# Colors for output
GREEN='\033[0;32m'
CYAN='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# Function to clear Redis keys
clear_redis() {
    if command -v redis-cli &> /dev/null; then
        redis-cli keys "limit:*$TEST_KEY*" | xargs redis-cli del &> /dev/null
    fi
}

# Function to run a burst test (Functional Verification)
run_burst_test() {
    local algo=$1
    local requests=$2
    local expected_allowed=$3
    local allowed=0
    local blocked=0
    local other=0
    
    echo -e "${CYAN}[Burst Test]${NC} Starting burst of $requests requests for $algo:"
    for ((i=1; i<=requests; i++)); do
        local url="$BASE_URL/test-limit?algo=$algo&key=$TEST_KEY&capacity=$CAPACITY&refillRate=$REFILL_RATE"
        resp=$(curl -s -o /dev/null -w "%{http_code}" "$url")
        
        printf "  Request %2d: " $i
        if [ "$resp" == "200" ]; then
            ((allowed++))
            echo -e "[${GREEN}ALLOWED (200)${NC}]"
        elif [ "$resp" == "429" ]; then
            ((blocked++))
            echo -e "[${RED}BLOCKED (429)${NC}]"
        else
            ((other++))
            echo -e "[${YELLOW}ERROR ($resp)${NC}]"
        fi
    done
    
    echo -e "\n  Summary for $algo:"
    echo -e "  - ${GREEN}Processed (Allowed)${NC}: $allowed"
    echo -e "  - ${RED}Blocked (Limited)${NC}: $blocked"
    if [ $other -gt 0 ]; then echo -e "  - ${YELLOW}Other Errors${NC}: $other"; fi
    echo -e "  - ${CYAN}Total Requests${NC}: $requests"

    if [ $allowed -eq $expected_allowed ] || [ $allowed -eq $((expected_allowed + 1)) ]; then
        echo -e "  STATUS: ${GREEN}PASS${NC}"
    else
        echo -e "  STATUS: ${RED}FAIL (Expected $expected_allowed allowed)${NC}"
    fi
    echo "--------------------------------------------------------"
}

# Function to run high-volume load test using Apache Bench
run_million_load_test() {
    local algo=$1
    echo -e "\n${YELLOW}>>> STARTING MASSIVE LOAD TEST (1,000,000 Requests) for $algo <<<${NC}"
    echo -e "${YELLOW}This may take a few minutes...${NC}"
    
    if ! command -v ab &> /dev/null; then
        echo -e "${RED}Error: 'ab' (Apache Bench) not found. Cannot run 1M test.${NC}"
        return 1
    fi

    clear_redis
    ab -n $LOAD_TEST_COUNT -c $LOAD_CONCURRENCY "$BASE_URL/test-limit?algo=$algo&key=load_test_1m&capacity=1000&refillRate=100"
    
    echo -e "\n${GREEN}1M Request Load Test for $algo Complete.${NC}"
}

echo -e "${GREEN}Starting Multi-Algorithm Automation & Load Test...${NC}"
echo "--------------------------------------------------------"

# 0. CHECK SERVER
echo -n "Checking if server is running on $BASE_URL... "
if ! curl -s --connect-timeout 2 "$BASE_URL/test-limit?algo=TOKEN_BUCKET&key=ping" > /dev/null; then
    echo -e "${RED}OFFLINE${NC}"
    echo -e "Error: The Spring Boot application is not running."
    echo -e "Please run ${YELLOW}mvn spring-boot:run${NC} in a separate terminal and try again."
    exit 1
fi
echo -e "${GREEN}ONLINE${NC}"

# 1. FUNCTIONAL PHASE (Burst/Refill tests for all algos)
echo -e "${YELLOW}PHASE 1: Functional Verification (Burst Tests)${NC}"
algorithms=("TOKEN_BUCKET" "FIXED_WINDOW" "LEAKY_BUCKET" "SLIDING_WINDOW_LOG" "SLIDING_WINDOW_COUNTER")

for algo in "${algorithms[@]}"; do
    clear_redis
    run_burst_test "$algo" 10 $CAPACITY
done

# 2. PERFORMANCE PHASE (1M Requests)
# By default, we run it for TOKEN_BUCKET as requested.
# To run for others, you can change the argument or loop them.
echo -e "\n${YELLOW}PHASE 2: High-Volume Performance Testing${NC}"
run_million_load_test "TOKEN_BUCKET"

echo -e "\n--------------------------------------------------------"
echo -e "${GREEN}All automated phases complete.${NC}"
