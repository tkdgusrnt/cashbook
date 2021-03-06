package com.gdu.cashbook.service;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gdu.cashbook.mapper.MemberMapper;
import com.gdu.cashbook.mapper.MemberidMapper;
import com.gdu.cashbook.vo.LoginMember;
import com.gdu.cashbook.vo.Member;
import com.gdu.cashbook.vo.MemberForm;
import com.gdu.cashbook.vo.Memberid;

@Service
@Transactional
public class MemberService {
	@Autowired private MemberMapper memberMapper;
	@Autowired private MemberidMapper memberidMapper;
	@Autowired private JavaMailSender javaMailSender;
	
	// 경로 : linux(/), windows(\\)
	@Value("D:\\git-cashbook\\cashbook\\src\\main\\resources\\static\\upload\\")
	private String path;
	
	public int getMemberPw(Member member) {
		// pw 추가
		UUID uuid = UUID.randomUUID(); // 랜덤문자열 생성 라이브러리
		String memberPw = uuid.toString().substring(0, 8);
		member.setMemberPw(memberPw);
		int row = memberMapper.updateMemberPw(member);
		if(row==1) {
			System.out.println(memberPw + " <-- update memberPw");
			// 메일로 update 성공한 랜덤 pw 전송 (JavaMailSender 객체 사용)
			SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
			simpleMailMessage.setTo(member.getMemberEmail());
			simpleMailMessage.setFrom("deokk95@gmial.com");
			simpleMailMessage.setSubject("Cashbook 비밀번호 찾기 결과");
			simpleMailMessage.setText("변경된 비밀번호 : " + memberPw);
			javaMailSender.send(simpleMailMessage);
		}
		return row;
	}
	
	// 이름, 전화번호, 이메일로 아이디 찾기
	public String getMemberIdByMember(Member member) {
		return memberMapper.selectMemberIdByMember(member);
	}
	
	// member 수정
	public int modifyMember(MemberForm memberForm) {
		//System.out.println("service" + memberForm.toString());
		String originMemberPic = memberMapper.selectMemberPic(memberForm.getMemberId());
		MultipartFile mf = memberForm.getMemberPic();
		String originName = mf.getOriginalFilename();
		System.out.println(originName + " <--originName");
		String memberPic = null;
		if(originName.equals("")) { // 파일 입력안되면 그전 파일 이름이랑 같게
			memberPic = originMemberPic;
		} else { // 확장자
			File originFile = new File(path+originMemberPic);
			// 새로운 파일 입력되면 그 전 파일 삭제
			if(originFile.exists() && !originMemberPic.equals("default.jpg")) {
				originFile.delete();
			}
			int lastDot = originName.lastIndexOf(".");
			String extension = originName.substring(lastDot);
			System.out.println(extension);
			memberPic = memberForm.getMemberId()+extension;
			System.out.println(memberPic);
		}
		System.out.println(memberPic);
		// MemberForm타입 -> Member타입으로 service로
		Member member = new Member();
		member.setMemberId(memberForm.getMemberId());
		member.setMemberPw(memberForm.getMemberPw());
		member.setMemberName(memberForm.getMemberName());
		member.setMemberPhone(memberForm.getMemberPhone());
		member.setMemberAddr(memberForm.getMemberAddr());
		member.setMemberEmail(memberForm.getMemberEmail());
		member.setMemberPic(memberPic);
		System.out.println(member + " <--memberService.modifyMember member");
		int row = memberMapper.updateMember(member);
		
		if(!originName.equals("")) {
			// file -> 물리적으로 저장			
			File file = new File(path+memberPic);
			try {
				mf.transferTo(file);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(); // 예외처리 안해도 되는 예외
			}
		}
		return row;
	}
	
	// member 수정 전 비밀번호 확인
	public Member getMemberByIdAndPw(LoginMember loginMember) {
		return memberMapper.selectMemberByIdAndPw(loginMember);
	}
	
	// member 삭제
	public int removeMember(LoginMember loginMember) {
		String memberPic = memberMapper.selectMemberPic(loginMember.getMemberId());
		File file = new File(path+memberPic);
		
		// 테이블에서 삭제
		int deleteResult = memberMapper.deleteMember(loginMember);
		int insertResult = 0;
		// 비밀번호 확인이 맞아서 삭제됬을때
		if(deleteResult==1) {
			// 삭제할 id memberid테이블에 추가
			Memberid memberid = new Memberid();
			memberid.setMemberid(loginMember.getMemberId());
			insertResult = memberidMapper.insertMemberid(memberid);
		}
		// member테이블에서 삭제 및 memberid테이블 추가 성공시 파일도 물리적으로 삭제
		if(insertResult==1 && file.exists() && !memberPic.equals("default.jpg")) {
			file.delete();
		}
		return insertResult;
	}
	
	// member 개인 정보
	public Member getMemberOne(LoginMember loginMember) {
		return memberMapper.selectMemberOne(loginMember);
	}
	
	// 회원가입 memberId 중복체크
	public String checkMemberId(String memberIdCheck) {
		return memberMapper.selectMemberId(memberIdCheck);
	}
	
	// 로그인 id, pw확인
	public LoginMember getLoginMember(LoginMember loginMember) {
		return memberMapper.selectLoginMember(loginMember);
	}
	
	// member 추가
	public int addMember(MemberForm memberForm) {
		//System.out.println("service" + member.toString());
		MultipartFile mf = memberForm.getMemberPic();
		// 확장자 필요(이미지 파일만 받고 싶으면 if(!mf.getContextType().equals("image/png")) 이런식으로 분기
		String originName = mf.getOriginalFilename();
		System.out.println(originName + " <--originName");
		String memberPic = null;
		if(originName.equals("")) {
			System.out.println(" originName == '' ");
			memberPic = "default.jpg";
		} else {
			int lastDot = originName.lastIndexOf(".");
			String extension = originName.substring(lastDot);
			System.out.println(extension);
			memberPic = memberForm.getMemberId()+extension;
			System.out.println(memberPic);
		}
		
		// MemberForm타입 -> Member타입으로 service로
		Member member = new Member();
		member.setMemberId(memberForm.getMemberId());
		member.setMemberPw(memberForm.getMemberPw());
		member.setMemberName(memberForm.getMemberName());
		member.setMemberPhone(memberForm.getMemberPhone());
		member.setMemberAddr(memberForm.getMemberAddr());
		member.setMemberEmail(memberForm.getMemberEmail());
		member.setMemberPic(memberPic);
		System.out.println(member + " <--memberService.addMember member");
		int row = memberMapper.insertMember(member);
		if(!originName.equals("")) {
			// file -> 물리적으로 저장			
			File file = new File(path+memberPic);
			System.out.println(path);
			try {
				mf.transferTo(file);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(); // 예외처리 안해도 되는 예외
			}
		}
		//return memberMapper.insertMember(member);
		return row;
	}
}
