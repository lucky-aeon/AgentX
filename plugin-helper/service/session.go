// Session
// 用于存储会话状态，包括接收的消息和处理结果
package service

import (
	"bufio"
	"fmt"
	"net/http"
	"strings"
	"sync"
	"time"
)

type McpMessage struct {
	McpName string
	Content string
	Type    string // "send" or "receive"
	Time    time.Time
}

type McpEndpoint struct {
	McpName  string
	Endpoint string
}

type Session struct {
	sync.RWMutex
	Id              string
	Results         []string
	Offset          int
	Receives        []string
	ReceiveOffset   int       // 新增接收消息的偏移量
	LastReceiveTime time.Time // 最后一次接收消息的时间

	// 消息历史记录
	messagesMutex sync.RWMutex
	messages      []McpMessage

	// MCP endpoints
	endpointsMutex sync.RWMutex
	endpoints      map[string]string // mcpName -> endpoint

	// SSE事件通道
	eventChan chan string
	doneChan  chan struct{}

	// SSE订阅
	sseWaitGroup sync.WaitGroup
}

func NewSession(id string) *Session {
	return &Session{
		Id:              id,
		LastReceiveTime: time.Now(),
		messages:        make([]McpMessage, 0),
		endpoints:       make(map[string]string),
		eventChan:       make(chan string, 100), // 缓冲通道，避免阻塞
		doneChan:        make(chan struct{}),
	}
}

func (s *Session) AddReceive(receive string) {
	s.Lock()
	defer s.Unlock()
	s.Receives = append(s.Receives, receive)
	s.LastReceiveTime = time.Now()
}

func (s *Session) AddResult(result string) {
	s.Lock()
	defer s.Unlock()
	s.Results = append(s.Results, result)
}

func (s *Session) GetId() string {
	return s.Id
}

func (s *Session) GetResults() []string {
	s.RLock()
	defer s.RUnlock()
	results := make([]string, len(s.Results))
	copy(results, s.Results)
	return results
}

func (s *Session) GetReceives() []string {
	s.RLock()
	defer s.RUnlock()
	receives := make([]string, len(s.Receives))
	copy(receives, s.Receives)
	return receives
}

func (s *Session) GetOffset() int {
	s.RLock()
	defer s.RUnlock()
	return s.Offset
}

func (s *Session) SetOffset(offset int) {
	s.Lock()
	defer s.Unlock()
	s.Offset = offset
}

// GetUnprocessedReceives 获取未处理的接收消息
func (s *Session) GetUnprocessedReceives() []string {
	s.Lock()
	defer s.Unlock()

	if s.ReceiveOffset >= len(s.Receives) {
		return nil
	}

	unprocessed := make([]string, len(s.Receives)-s.ReceiveOffset)
	copy(unprocessed, s.Receives[s.ReceiveOffset:])
	s.ReceiveOffset = len(s.Receives)
	return unprocessed
}

// GetUnreadResults 获取未读取的处理结果
func (s *Session) GetUnreadResults() []string {
	s.Lock()
	defer s.Unlock()

	if s.Offset >= len(s.Results) {
		return nil
	}

	unread := make([]string, len(s.Results)-s.Offset)
	copy(unread, s.Results[s.Offset:])
	s.Offset = len(s.Results)
	return unread
}

// AddMessage 添加一条消息记录
func (s *Session) AddMessage(mcpName string, content string, msgType string) {
	s.messagesMutex.Lock()
	defer s.messagesMutex.Unlock()

	s.messages = append(s.messages, McpMessage{
		McpName: mcpName,
		Content: content,
		Type:    msgType,
		Time:    time.Now(),
	})
}

// GetMessages 获取所有消息记录
func (s *Session) GetMessages() []McpMessage {
	s.messagesMutex.RLock()
	defer s.messagesMutex.RUnlock()

	// 返回消息记录的副本
	messages := make([]McpMessage, len(s.messages))
	copy(messages, s.messages)
	return messages
}

// AddEndpoint 添加MCP的endpoint
func (s *Session) AddEndpoint(mcpName string, endpoint string) {
	s.endpointsMutex.Lock()
	defer s.endpointsMutex.Unlock()
	s.endpoints[mcpName] = endpoint
}

// GetEndpoint 获取MCP的endpoint
func (s *Session) GetEndpoint(mcpName string) (string, bool) {
	s.endpointsMutex.RLock()
	defer s.endpointsMutex.RUnlock()
	endpoint, exists := s.endpoints[mcpName]
	return endpoint, exists
}

// GetEndpoints 获取所有endpoints
func (s *Session) GetEndpoints() map[string]string {
	s.endpointsMutex.RLock()
	defer s.endpointsMutex.RUnlock()

	endpoints := make(map[string]string)
	for k, v := range s.endpoints {
		endpoints[k] = v
	}
	return endpoints
}

// SubscribeSSE 订阅MCP服务的SSE事件
func (s *Session) SubscribeSSE(mcpName string, sseUrl string) {
	s.sseWaitGroup.Add(1)
	go func() {
		defer s.sseWaitGroup.Done()

		resp, err := http.Get(sseUrl)
		if err != nil {
			return
		}
		defer resp.Body.Close()

		reader := bufio.NewReader(resp.Body)
		var currentEvent string

		for {
			select {
			case <-s.doneChan:
				return
			default:
				line, err := reader.ReadString('\n')
				if err != nil {
					return
				}
				line = strings.TrimSpace(line)

				if line == "" {
					continue
				}

				if strings.HasPrefix(line, "event: ") {
					currentEvent = strings.TrimPrefix(line, "event: ")
				} else if strings.HasPrefix(line, "data: ") {
					data := strings.TrimPrefix(line, "data: ")

					// 如果是endpoint事件，保存endpoint
					if currentEvent == "endpoint" {
						s.AddEndpoint(mcpName, data)
					}

					// 记录接收到的消息
					s.AddMessage(mcpName, data, "receive")

					// 如果不是endpoint事件，转发给客户端
					if currentEvent != "endpoint" {
						s.SendEvent(fmt.Sprintf("data: %s\n\n", data))
					}
				}
			}
		}
	}()
}

// Close 关闭会话
func (s *Session) Close() {
	close(s.doneChan)
	s.sseWaitGroup.Wait() // 等待所有SSE订阅goroutine结束
}

// SendEvent 发送SSE事件
func (s *Session) SendEvent(event string) {
	select {
	case s.eventChan <- event:
	default:
		// 如果通道已满，丢弃事件
	}
}

// GetEventChan 获取事件通道
func (s *Session) GetEventChan() <-chan string {
	return s.eventChan
}

// GetDoneChan 获取关闭通道
func (s *Session) GetDoneChan() <-chan struct{} {
	return s.doneChan
}
