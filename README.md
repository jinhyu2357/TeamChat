# TeamChat

Paper 서버(마인크래프트 1.21 계열)용 Kotlin 플러그인 프로젝트입니다.  

## 프로젝트 개요

- 플러그인 이름: `TeamChat`
- 메인 클래스: `org.example.jinhhyu.teamchat.TeamChat`
- 대상 API 버전: `1.21`
- 언어/런타임: Kotlin + JVM(Java 21)
- 빌드 도구: Gradle(Kotlin DSL)

## 현재 포함된 기능
`plugin.yml`에 `/t` 명령어가 선언되어 있습니다.

- 명령어: `/t`
- 설명: Team chat and team management
- 사용법: `/t <invite|join|leave|chat|message>`



## 다음 구현 권장 사항

- `/t` 명령어 Executor 및 TabCompleter 구현
- 팀 생성/초대/가입/탈퇴 데이터 구조 설계
- 팀 채팅 채널 분리 및 메시지 라우팅
- 권한 노드(`permissions`)와 사용자 피드백 메시지 정비
- 서버 재시작 이후 팀 상태 유지를 위한 저장소(YAML/DB) 연동
