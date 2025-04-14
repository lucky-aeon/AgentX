package xlog

import (
	"fmt"
	"os"
	"path/filepath"
)

func CreateLogDir(baseDir string) error {
	// 创建 logs 目录
	if err := os.MkdirAll(filepath.Join(baseDir, "logs"), 0755); err != nil {
		return fmt.Errorf("failed to create logs directory: %w", err)
	}
	return nil
}

func CreateLogFile(baseDir, fileName string) (*os.File, error) {
	err := CreateLogDir(baseDir)
	if err != nil {
		return nil, err
	}

	file, err := os.OpenFile(filepath.Join(baseDir, "logs", fileName), os.O_CREATE|os.O_APPEND|os.O_WRONLY, 0644)
	if err != nil {
		return nil, fmt.Errorf("failed to create log file: %w", err)
	}
	return file, nil
}
