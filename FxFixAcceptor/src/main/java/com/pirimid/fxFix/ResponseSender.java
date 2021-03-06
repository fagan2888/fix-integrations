package com.pirimid.fxFix;

import com.pirimid.utils.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix44.*;
import quickfix.fix44.Message;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.pirimid.utils.Constants.NDF;

public class ResponseSender {

    private static final Logger logger = LoggerFactory.getLogger(ResponseSender.class);
    public static final String SAMPLE_SETTL_DATE = "20171117";

    public Map<String, MarketDataRequest> subscribedRequests = new ConcurrentHashMap<>();
    private boolean sendingMarketDataResponseStarted = false;

    public void startSendingMarketDataRefreshResponseIfNotStarted(SessionID sessionID) {
        if(sendingMarketDataResponseStarted) {
            return;
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (Session.lookupSession(sessionID).hasResponder()) {
                        subscribedRequests.values().forEach(order -> sendMarketDataRefreshRequestForOrder(order, sessionID));
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            logger.error("Thread execution interrupted", e);
                        }
                    }
                }
            });
            thread.start();
            sendingMarketDataResponseStarted = true;
        }
    }

    private void sendMarketDataRefreshRequestForOrder(MarketDataRequest order, SessionID sessionID) {
        MDUpdateType mdUpdateType = new MDUpdateType();
        int mdUpdateTypeValue = 1;
        try {
            mdUpdateTypeValue = order.get(mdUpdateType).getValue();
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Field {} not found in order {}", mdUpdateType.getField(), order.toString());
        }

        if (isIncrementalRefreshRequested(mdUpdateTypeValue)) {
            sendMarketDataIncrementalRefreshToClient(order, sessionID);
        } else {
            sendMarketDataFullRefreshToClient(order, sessionID);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.error("Thread execution interrupted", e);
        }
    }

    public void sendMarketDataFullRefreshToClient(MarketDataRequest order, SessionID sessionID) {
        List<Group> groups = order.getGroups(NoRelatedSym.FIELD);

        for (Group group : groups) {
            MarketDataSnapshotFullRefresh marketDataSnapshotFullRefresh = prepareMarketDataSnapshotFullRefreshRequest(order, group);
            sendMessageToTarget(marketDataSnapshotFullRefresh, sessionID);
        }
    }

    private MarketDataSnapshotFullRefresh prepareMarketDataSnapshotFullRefreshRequest(MarketDataRequest order, Group group) {
        MarketDataSnapshotFullRefresh marketDataSnapshotFullRefresh = null;
        try {
            Symbol symbol = new Symbol(group.getString(Symbol.FIELD));
            List<Group> mdEntries = order.getGroups(NoMDEntryTypes.FIELD);
            marketDataSnapshotFullRefresh = new MarketDataSnapshotFullRefresh();
            marketDataSnapshotFullRefresh.set(symbol);
            marketDataSnapshotFullRefresh.set(order.getMDReqID());
            marketDataSnapshotFullRefresh.set(new NoMDEntries(mdEntries.size()));
            marketDataSnapshotFullRefresh.setField(order.getMarketDepth());
            String settlTypeValue = SettlType.REGULAR;
            if (isSetField(group, SettlType.FIELD)) {
                settlTypeValue = group.getField(new SettlType()).getValue();
                marketDataSnapshotFullRefresh.setField(group.getField(new SettlType()));
            }
            String settlDateValue = SAMPLE_SETTL_DATE;
            if (isSetField(group, SettlDate.FIELD)) {
                settlDateValue = group.getField(new SettlDate()).getValue();
            }
            marketDataSnapshotFullRefresh.setField(new SettlDate(settlDateValue));
            if (isSetField(group, MaturityDate.FIELD)) {
                marketDataSnapshotFullRefresh.setField(group.getField(new MaturityDate()));
            }
            if (isSetField(group, NDF)) {
                marketDataSnapshotFullRefresh.setField(group.getField(new CharField(NDF)));
            }
            for (int i = 0; i < mdEntries.size(); i++) {
                MarketDataSnapshotFullRefresh.NoMDEntries mdEntryGroup = (MarketDataSnapshotFullRefresh.NoMDEntries) prepareMDEntryGroup(mdEntries.get(i), i, isSpotRequest(settlTypeValue), true);
                marketDataSnapshotFullRefresh.addGroup(mdEntryGroup);
            }
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Field {} not found", fieldNotFound.field, fieldNotFound);
        }
        return marketDataSnapshotFullRefresh;
    }

    public void sendMarketDataIncrementalRefreshToClient(MarketDataRequest order, SessionID sessionID) {
        List<Group> groups = order.getGroups(NoRelatedSym.FIELD);

        for (Group group : groups) {
            MarketDataIncrementalRefresh marketDataIncrementalRefresh = prepareMarketDataIncrementalRefreshRequest(order, group);
            sendMessageToTarget(marketDataIncrementalRefresh, sessionID);
        }
    }

    private MarketDataIncrementalRefresh prepareMarketDataIncrementalRefreshRequest(MarketDataRequest order, Group group) {
        MarketDataIncrementalRefresh marketDataIncrementalRefresh = null;
        try {
            List<Group> mdEntries = order.getGroups(NoMDEntryTypes.FIELD);
            Symbol symbol = new Symbol(group.getString(Symbol.FIELD));
            marketDataIncrementalRefresh = new MarketDataIncrementalRefresh();
            marketDataIncrementalRefresh.setField(symbol);
            marketDataIncrementalRefresh.set(order.getMDReqID());
            marketDataIncrementalRefresh.set(new NoMDEntries(mdEntries.size()));
            marketDataIncrementalRefresh.setField(order.getMarketDepth());
            String settlTypeValue = SettlType.REGULAR;
            if (isSetField(group, SettlType.FIELD)) {
                settlTypeValue = group.getField(new SettlType()).getValue();
                marketDataIncrementalRefresh.setField(group.getField(new SettlType()));
            }
            String settlDateValue = SAMPLE_SETTL_DATE;
            if (isSetField(group, SettlDate.FIELD)) {
                settlDateValue = group.getField(new SettlDate()).getValue();
            }
            marketDataIncrementalRefresh.setField(new SettlDate(settlDateValue));
            if (isSetField(group, MaturityDate.FIELD)) {
                marketDataIncrementalRefresh.setField(group.getField(new MaturityDate()));
            }
            if (isSetField(group, NDF)) {
                marketDataIncrementalRefresh.setField(group.getField(new CharField(NDF)));
            }
            for (int i = 0; i < mdEntries.size(); i++) {
                MarketDataIncrementalRefresh.NoMDEntries mdEntryGroup = (MarketDataIncrementalRefresh.NoMDEntries) prepareMDEntryGroup(mdEntries.get(i), i, isSpotRequest(settlTypeValue), false);
                marketDataIncrementalRefresh.addGroup(mdEntryGroup);
            }
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Field {} not found", fieldNotFound.field, fieldNotFound);
        }
        return marketDataIncrementalRefresh;
    }

    private Group prepareMDEntryGroup(Group entryGroup, int index, boolean isSpotRequest, boolean isFullRefresh) throws FieldNotFound {
        Double price = Helper.generateNextPrice();
        Group mdEntryGroup;
        if(isFullRefresh) {
            mdEntryGroup = new MarketDataSnapshotFullRefresh.NoMDEntries();
        } else {
            mdEntryGroup = new MarketDataIncrementalRefresh.NoMDEntries();
        }
        mdEntryGroup.setField(new MDUpdateAction('0'));
        char mdEntryTypeValue = entryGroup.getField(new MDEntryType()).getValue();
        mdEntryGroup.setField(new MDEntryType(mdEntryTypeValue));
        if (isBidEntry(mdEntryTypeValue)) {
            if (isSpotRequest) {
                mdEntryGroup.setField(new BidSpotRate(price));
            } else {
                mdEntryGroup.setField(new BidForwardPoints(price));
            }
        } else {
            if (isSpotRequest) {
                mdEntryGroup.setField(new OfferSpotRate(price));
            } else {
                mdEntryGroup.setField(new OfferForwardPoints(price));
            }
        }
        mdEntryGroup.setField(new MDEntryID("MDEntryId" + index));
        mdEntryGroup.setField(new MDEntryPx(price));
        mdEntryGroup.setField(new MDEntrySize(10000000));
        mdEntryGroup.setField(new QuoteEntryID("QuoteEntryId" + index));
        mdEntryGroup.setField(new MDEntryPositionNo(4));
        mdEntryGroup.setField(new MDQuoteType(1));
        return mdEntryGroup;
    }

    public void sendExecutionReportToClient(NewOrderSingle order, SessionID sessionID) {
        ExecutionReport accept = prepareExecutionReport(order);
        logger.info("###Sending Order Acceptance:" + accept.toString() + "sessionID:" + sessionID.toString());
        sendMessageToTarget(accept, sessionID);
    }

    private ExecutionReport prepareExecutionReport(NewOrderSingle order) {
        ExecutionReport accept = null;
        try {
            accept = new ExecutionReport(new OrderID("133456"), new ExecID("789"),
                    new ExecType(ExecType.FILL), new OrdStatus(OrdStatus.FILLED), order.getSide(), new LeavesQty(2),
                    new CumQty(0), new AvgPx(order.getPrice().getValue()));
            accept.set(order.getClOrdID());
            accept.set(order.getAccount());
            accept.set(order.getSymbol());
            accept.set(order.getOrderQty());
            accept.set(order.getOrdType());
            accept.set(order.getPrice());
            accept.set(order.getCurrency());
            accept.set(order.getTimeInForce());
            accept.set(new LastQty(order.getOrderQty().getValue()));
            accept.set(new LastPx(order.getPrice().getValue()));
            accept.set(new LastSpotRate(order.getPrice().getValue()));
            accept.set(order.getTransactTime());
            accept.set(new TradeDate("20180927"));
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Field {} not found", fieldNotFound.field, fieldNotFound);
        }
        return accept;
    }

    private void sendMessageToTarget(Message message, SessionID sessionID) {
        try {
            Session.sendToTarget(message, sessionID);
        } catch (SessionNotFound sessionNotFound) {
            logger.error("Session not found for session id: {} : {}", sessionID.toString(), sessionNotFound);
        }
    }

    public void subscribeNewMarketDataRequest(MarketDataRequest request) {
        try {
            String MDReqId = request.getMDReqID().getValue();
            this.subscribedRequests.put(MDReqId, request);
        } catch (FieldNotFound fieldNotFound) {
            logger.error("Field {} not found", fieldNotFound.field, fieldNotFound);
        }
    }

    public void unsubscribeMarketDataRequest(String reqId) {
        if(this.subscribedRequests.containsKey(reqId)) {
            this.subscribedRequests.remove(reqId);
        }
    }

    private boolean isIncrementalRefreshRequested(int mdUpdateTypeValue) {
        return mdUpdateTypeValue == 1;
    }

    private boolean isBidEntry(char mdEntryTypeValue) {
        return mdEntryTypeValue == '0';
    }

    private boolean isSpotRequest(String settlTypeValue) {
        return "0".equals(settlTypeValue);
    }

    private boolean isSetField(Group group, int field) {
        return group.isSetField(field);
    }

}
