# TeamChat (ChatRange)

Paper 서버(마인크래프트 1.21 계열)용 Kotlin 플러그인 프로젝트입니다.  
현재 코드는 **기본 플러그인 골격과 명령어 선언이 준비된 초기 상태**이며, 팀 채팅 기능은 아직 구현되지 않았습니다.

## 프로젝트 개요

- 플러그인 이름: `ChatRange`
- 메인 클래스: `org.example.jinhhyu.chatRange.ChatRange`
- 대상 API 버전: `1.21`
- 언어/런타임: Kotlin + JVM(Java 21)
- 빌드 도구: Gradle(Kotlin DSL)

## 현재 포함된 기능

### 1) 플러그인 라이프사이클 연결
`ChatRange` 클래스가 `JavaPlugin`을 상속하고, 아래 라이프사이클 메서드를 오버라이드합니다.

- `onEnable()` : 서버에서 플러그인 활성화 시 호출
- `onDisable()` : 서버에서 플러그인 비활성화 시 호출

> 현재 두 메서드 내부 로직은 비어 있어 실제 동작은 없습니다.

### 2) 명령어 등록(선언)
`plugin.yml`에 `/t` 명령어가 선언되어 있습니다.

- 명령어: `/t`
- 설명: Team chat and team management
- 사용법: `/t <invite|join|leave|chat|message>`

> 주의: 현재 Kotlin 코드에는 해당 명령어 실행 로직(Executor/TabCompleter)이 구현되어 있지 않습니다. 따라서 선언만 되어 있고, 실제 팀 기능은 동작하지 않습니다.

## 작동 방식(현재 기준)

1. 서버가 시작되며 플러그인을 로드합니다.
2. Paper가 `ChatRange` 메인 클래스를 통해 플러그인을 초기화합니다.
3. `onEnable()`이 호출되지만, 내부 비즈니스 로직이 없어 추가 동작 없이 유지됩니다.
4. 서버 종료/리로드 시 `onDisable()`이 호출되지만, 내부 정리 로직은 아직 없습니다.

즉, **"플러그인 로딩/언로딩 구조는 연결되어 있으나, 팀 채팅 기능은 미구현"** 상태입니다.

## 개발/실행 방법

### 요구 사항

- JDK 21
- Gradle Wrapper(프로젝트 포함)

### 빌드

```bash
./gradlew build
```

빌드 시 `shadowJar`를 포함하도록 설정되어 있어 플러그인 JAR 패키징이 함께 수행됩니다.

### 로컬 테스트 서버 실행

```bash
./gradlew runServer
```

- `run-paper` 플러그인 설정으로 마인크래프트 `1.21` 서버를 실행합니다.
- 빌드된 플러그인 JAR이 자동으로 서버에 반영됩니다.

## 프로젝트 구조

```text
src/main/kotlin/org/example/jinhhyu/chatRange/ChatRange.kt   # 메인 플러그인 클래스
src/main/resources/plugin.yml                                # Bukkit/Paper 플러그인 메타데이터 + 명령어 선언
src/main/resources/paper-plugin.yml                          # Paper 플러그인 메타데이터
build.gradle.kts                                             # 빌드/의존성/실행 태스크 설정
```

## 다음 구현 권장 사항

- `/t` 명령어 Executor 및 TabCompleter 구현
- 팀 생성/초대/가입/탈퇴 데이터 구조 설계
- 팀 채팅 채널 분리 및 메시지 라우팅
- 권한 노드(`permissions`)와 사용자 피드백 메시지 정비
- 서버 재시작 이후 팀 상태 유지를 위한 저장소(YAML/DB) 연동
