package com.springboot.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.springboot.dao.UserRepository;
import com.springboot.entities.User;
//import com.springboot.helper.Message;
import com.springboot.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {
	
	Random random=new Random(1000);

	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;	
	@RequestMapping("/forgot")
	public String openEmailForm() {
		
		return "forgot_email_form";
		
	}

	@PostMapping("/send-otp")
	public String sendOTP( @RequestParam("email") String email, HttpSession session) {
				
//	    generating otp of four digit(MAX-LIMIT)
		int otp=random.nextInt(999999);
		
		System.out.println("OTP: "+otp);
		
//		code for send otp to email
		
		String subject="OTP from Smart Contact Manager";
		
		
		String message = "<h1> OTP is = "+otp+"</h1>";
				
		String to=email;
		boolean flag=this.emailService.sendEmail(subject, message, to);
		
		if(flag) {
			
//			storing/saving  opt in session if OTP is correct or not
			
			session.setAttribute("myotp",otp);
			session.setAttribute("email", email);
			
			return "verify_otp";
		}else {
			session.setAttribute("message","Check your email");
			
			return "forgot_email_form";
		}	
	}
	
//	verify-otp
	
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp,HttpSession session) {
		
		int myOtp=(int)session.getAttribute("myotp");
		String email=(String)session.getAttribute("email");
		
		
		if(myOtp==otp) {
			 
			User user=this.userRepository.getUserByUserName(email);
			
			if(user==null) {
				session.setAttribute("message","User does not exist with this email!!");
				
				return "forgot_email_form";
			}else {
				
			}
			return "password_change_form";
		}else {
			
			session.setAttribute("message", "You have Entered wrong otp");
			
			return "verify_otp";
		}
	}
	
//	change-password
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newPassword") String newPassword, HttpSession session) {
		
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		
		user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
		this.userRepository.save(user);
	    
		
		return "redirect:/signin?change=password changed successfully..";
	}
}
