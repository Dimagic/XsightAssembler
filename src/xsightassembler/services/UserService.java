package xsightassembler.services;

import xsightassembler.dao.UserDao;
import xsightassembler.models.User;
import xsightassembler.utils.CustomException;

import java.util.List;

public class UserService {
    private final UserDao dao = new UserDao();

    public User findById(long id) throws CustomException {
        return dao.findById(id);
    }

    public List<User> findAll() throws CustomException {
        return dao.findAll();
    }

    public boolean save(User user) throws CustomException {
        return dao.save(user);
    }

    public boolean saveOrUpdate(User user) throws CustomException {
        return dao.saveOrUpdate(user);
    }

    public boolean delete(User user) throws CustomException {
        return dao.delete(user);
    }

    public  boolean update(User user) throws  CustomException {
        return dao.update(user);
    }

    public User findByLogin(String login) throws CustomException {
        return dao.findByLogin(login);
    }
}
