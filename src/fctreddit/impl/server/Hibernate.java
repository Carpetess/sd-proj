package fctreddit.impl.server;

import java.io.File;
import java.util.List;

import fctreddit.api.data.Post;
import fctreddit.api.data.Vote;
import jakarta.persistence.RollbackException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

/**
 * A helper class to perform POJO (Plain Old Java Objects) persistence, using Hibernate and a backing relational database.
 */
public class Hibernate {
    private static final String HIBERNATE_CFG_FILE = "hibernate.cfg.xml";
    private SessionFactory sessionFactory;
    private static Hibernate instance;




    private Hibernate() {
        try {
            sessionFactory = new Configuration()
                    .configure(new File(HIBERNATE_CFG_FILE))
                    .buildSessionFactory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the Hibernate instance, initializing if necessary.
     * Requires a configuration file (hibernate.cfg.xml)
     * @return
     */
    synchronized public static Hibernate getInstance() {
        if (instance == null)
            instance = new Hibernate();
        return instance;
    }

    public class TX {
        public final Session session;
        public final Transaction tx;

        public TX(Session session, Transaction tx) {
            this.session = session;
            this.tx = tx;
        }
    }

    public TX beginTransaction() {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        return new TX(session, tx);
    }

    public void commitTransaction(TX tx) throws RollbackException {
        tx.tx.commit();
        tx.session.close();
    }

    public void abortTransaction(TX tx) {
        if(tx.tx != null && tx.tx.isActive())
            tx.tx.rollback();
        if(tx.session != null && tx.session.isOpen())
            tx.session.close();
    }


    /**
     * Persists one or more objects to storage
     * @param objects - the objects to persist
     */
    public void persist(Object... objects) {
        Transaction tx = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for( var o : objects )
                session.persist(o);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
    }

    public void persist(TX tx, Object... objects) {
        for(Object o: objects) {
            tx.session.persist(o);
        }
    }


    /**
     * Gets one object from storage
     * @param identifier - the objects identifier
     * @param clazz - the class of the object that to be returned
     */
    public <T> T get(Class<T> clazz, Object identifier) {
        Transaction tx = null;
        T element = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            element = session.get(clazz, identifier);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
        return element;
    }

    public <T> T get(TX tx, Class<T> clazz, Object identifier) {
        return tx.session.get(clazz, identifier);
    }

    public void update(TX tx, Object... objects) {
        for(Object o : objects) {
            tx.session.update(o);
        }
    }

    /**
     * Updates one or more objects previously persisted.
     * @param objects - the objects to update
     */
    public void update(Object... objects) {
        Transaction tx = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for( var o : objects )
                session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
    }

    public void delete(TX tx, Object... objects) {
        for(Object o : objects) {
            tx.session.delete(o);
        }
    }
    /**
     * Removes one or more objects from storage
     * @param objects - the objects to remove from storage
     */
    public void delete(Object... objects) {
        Transaction tx = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for( var o : objects )
                session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
    }

    public void deleteAll(TX tx, List<?> objects) {
        for (Object o : objects)
            tx.session.delete(o);
    }
    public void deleteAll(List<?> objects) {
        Transaction tx = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for( var o : objects )
                session.remove(o);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
    }

    public void updateAll(TX tx, List<?> objects) {
        for (Object o : objects) {
            tx.session.merge(o);
        }
    }
    public void updateAll(List<?> objects) {
        Transaction tx = null;
        try(var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            for( var o : objects )
                session.merge(o);
            tx.commit();
        } catch (Exception e) {
            if (tx!=null) tx.rollback();
            throw e;
        }
    }


    /**
     * Performs a jpql Hibernate query (SQL dialect)
     * @param <T> The type of objects returned by the query
     * @param jpqlStatement - the jpql query statement
     * @param clazz - the class of the objects that will be returned
     * @return - list of objects that match the query
     */
    public <T> List<T> jpql(String jpqlStatement, Class<T> clazz) {
        try(var session = sessionFactory.openSession()) {
            var query = session.createQuery(jpqlStatement, clazz);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }
    public <T> List<T> jpql(TX tx, String jpqlStatement, Class<T> clazz) {
        Query<T> query = tx.session.createQuery(jpqlStatement, clazz);
        return query.list();
    }

    /**
     * Performs a (native) SQL query
     *
     * @param <T> The type of objects returned by the query
     * @param jpqlStatement - the sql query statement
     * @param clazz - the class of the objects that will be returned
     * @return - list of objects that match the query
     */
    public <T> List<T> sql(String jpqlStatement, Class<T> clazz) {
        try(var session = sessionFactory.openSession()) {
            var query = session.createNativeQuery(jpqlStatement, clazz);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }



    public void executeSQL (String sqlStatement) {
        Transaction tx = null;
            try (var session = sessionFactory.openSession()) {
                tx = session.beginTransaction();
                var query = session.createNativeQuery(sqlStatement);
                query.executeUpdate();
                tx.commit();
            } catch (Exception e) {
                throw e;

            }
    }

    public <T> List<T> sql(TX tx, String sqlStatement, Class<T> clazz) {
        Query<T> query = tx.session.createNativeQuery(sqlStatement, clazz);
        return query.list();
    }

    public int sql(TX tx, String sqlStatement) {
        @SuppressWarnings("deprecation")
        Query<?> query = tx.session.createNativeQuery(sqlStatement);
        return query.executeUpdate();
    }


}