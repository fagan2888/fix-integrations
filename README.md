# FIX-Integrations
FIX client/server sample app for FX market data and trading using quickFix engine.

[![Build Status](https://travis-ci.com/Pirimid/fix-integrations.svg?branch=master)](https://travis-ci.com/Pirimid/fix-integrations)

## Types of Requests that are supported by this Sample Application

### Initiator
 1. Market Data Spot Request
    1. Full Refresh
    2. Incremental Refresh
    3. Unsubscribe
2. Market Data Forward Request
    1. Full Refresh
    2. Incremental Refresh
    3. Unsubscribe
3. Market Data NDF Request
    1. Full Refresh
    2. Incremental Refresh
    3. Unsubscribe
4. New Order Single Request

### Acceptor
1. Market Data Full Refresh
    1. Spot
    2. Forward
    3. NDF
2. Market Data Incremental Refresh
    1. Spot
    2. Forward
    3. NDF
3. Market Data Unsubscribe
    1. Spot
    2. Forward
    3. NDF
4. Execution Report
