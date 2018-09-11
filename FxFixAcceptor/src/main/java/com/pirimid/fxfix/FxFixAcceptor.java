package com.pirimid.fxfix;

import com.pirimid.utility.PriceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.*;
import quickfix.fix42.MarketDataRequest;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
import quickfix.fix42.NewOrderSingle;

import java.util.List;

/**
 * A Fix Acceptor implementation for FX.
 */
public class FxFixAcceptor extends MessageCracker implements Application {

    private static final Logger logger = LoggerFactory.getLogger(FxFixAcceptor.class);

    @Override
    public void onCreate(SessionID sessionID) {

    }

    @Override
    public void onLogon(SessionID sessionID) {

    }

    @Override
    public void onLogout(SessionID sessionID) {

    }

    @Override
    public void toAdmin(Message message, SessionID sessionID) {

    }

    @Override
    public void fromAdmin(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
        System.out.println("Admin Message Received (Acceptor) :" + message.toString());
    }

    @Override
    public void toApp(Message message, SessionID sessionID) throws DoNotSend {

    }

    @Override
    public void fromApp(Message message, SessionID sessionID) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        crack(message, sessionID);
    }

    public void onMessage(NewOrderSingle order, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        System.out.println("###NewOrder Received:" + order.toString());
        System.out.println("###Symbol" + order.getSymbol().toString());
        System.out.println("###Side" + order.getSide().toString());
        System.out.println("###Type" + order.getOrdType().toString());
        System.out.println("###TransactioTime" + order.getTransactTime().toString());

        sendMessageToClient(order, sessionID);
    }

    public void onMessage(quickfix.fix42.MarketDataRequest order, SessionID sessionID) throws FieldNotFound {
        System.out.println("###New Market Data Request Order Received: " + order.toString());
        System.out.println("###Market Data Request Id: " + order.getMDReqID().toString());
        System.out.println("###Subscriptoin Request Type: " + order.getSubscriptionRequestType().toString());
        System.out.println("###Market Depth: " + order.getMarketDepth().toString());

        sendMarketDataFullRefreshToClient(order, sessionID);

    }

    public void sendMessageToClient(quickfix.fix42.NewOrderSingle order, SessionID sessionID) {
        try {
            OrderQty orderQty = null;

            orderQty = new OrderQty(56.0);
            quickfix.fix40.ExecutionReport accept = new quickfix.fix40.ExecutionReport(new OrderID("133456"), new ExecID("789"),
                    new ExecTransType(ExecTransType.NEW), new OrdStatus(OrdStatus.NEW), order.getSymbol(), order.getSide(),
                    orderQty, new LastShares(0), new LastPx(0), new CumQty(0), new AvgPx(0));
            accept.set(order.getClOrdID());
            System.out.println("###Sending Order Acceptance:" + accept.toString() + "sessionID:" + sessionID.toString());
            Session.sendToTarget(accept, sessionID);
        } catch (RuntimeException e) {
            LogUtil.logThrowable(sessionID, e.getMessage(), e);
        } catch (FieldNotFound fieldNotFound) {
            fieldNotFound.printStackTrace();
        } catch (SessionNotFound sessionNotFound) {
            sessionNotFound.printStackTrace();
        }
    }

    public void sendMarketDataFullRefreshToClient(MarketDataRequest order, SessionID sessionID) {
        List<Group> groups = order.getGroups(NoRelatedSym.FIELD);

        for (Group group : groups) {
            try {
                Symbol symbol = new Symbol(group.getString(Symbol.FIELD));
                List<Group> mdEntries = order.getGroups(NoMDEntryTypes.FIELD);
                MarketDataSnapshotFullRefresh m = new MarketDataSnapshotFullRefresh(symbol);
                m.set(order.getMDReqID());
                m.set(new NoMDEntries(mdEntries.size()));
                m.setField(order.getMarketDepth());
                for (int i = 0; i < mdEntries.size(); i++) {
                    Group entry = mdEntries.get(i);
                    MarketDataSnapshotFullRefresh.NoMDEntries mdEntryGroup = new MarketDataSnapshotFullRefresh.NoMDEntries();
                    mdEntryGroup.set(new MDEntryType('0'));
                    mdEntryGroup.setField(new MDEntryID("MDEntryId" + i));
                    mdEntryGroup.set(new MDEntryPx(123.123));
                    mdEntryGroup.set(new MDEntrySize(10000000));
                    mdEntryGroup.set(new QuoteEntryID("QuoteEntryId" + i));
                    mdEntryGroup.set(new MDEntryPositionNo(4));
                    mdEntryGroup.setField(new MDQuoteType(1));
                    mdEntryGroup.set(new MDEntryType('1'));
                    m.addGroup(mdEntryGroup);
                }
                SettlDate settlDate = new SettlDate("20171117");
                m.setField(settlDate);

                Session.sendToTarget(m, sessionID);
            } catch (FieldNotFound fieldNotFound) {
                fieldNotFound.printStackTrace();
            } catch (SessionNotFound sessionNotFound) {
                sessionNotFound.printStackTrace();
            }
        }
    }
}
