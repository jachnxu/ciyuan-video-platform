package com.ciyuan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ciyuan.common.BizException;
import com.ciyuan.common.UserContext;
import com.ciyuan.common.JwtUtil;
import com.ciyuan.dto.Requests;
import com.ciyuan.entity.User;
import com.ciyuan.mapper.UserMapper;
import com.ciyuan.vo.Vos;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public Vos.LoginResult register(Requests.Register req) {
        String username = req.getUsername().trim();
        if (!username.matches("^[a-zA-Z0-9_]{3,20}$")) {
            throw new BizException("用户名只能包含字母、数字、下划线，3-20 位");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exists != null && exists > 0) throw new BizException("用户名已被占用");

        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(req.getPassword()));
        u.setNickname(StringUtils.hasText(req.getNickname()) ? req.getNickname().trim() : username);
        u.setSign("这个人很神秘，什么都没有写");
        u.setGender(0);
        u.setCoin(5);
        u.setLevel(0);
        u.setExp(0);
        u.setRole("USER");
        u.setStatus(0);
        userMapper.insert(u);

        Vos.LoginResult r = new Vos.LoginResult();
        r.setToken(jwtUtil.create(u.getId(), u.getRole()));
        r.setUser(userService.profile(u.getId()));
        return r;
    }

    public Vos.LoginResult login(Requests.Login req) {
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername().trim()));
        if (u == null || !encoder.matches(req.getPassword(), u.getPassword())) {
            throw new BizException("账号或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 1) throw new BizException(403, "账号已被封禁");
        Vos.LoginResult r = new Vos.LoginResult();
        r.setToken(jwtUtil.create(u.getId(), u.getRole()));
        r.setUser(userService.profile(u.getId()));
        return r;
    }

    public Vos.UserVO current() {
        Long id = UserContext.id();
        if (id == null) return null;
        return userService.profile(id);
    }
}
